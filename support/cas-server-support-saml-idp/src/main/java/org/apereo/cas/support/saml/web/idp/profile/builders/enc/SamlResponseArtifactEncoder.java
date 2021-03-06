package org.apereo.cas.support.saml.web.idp.profile.builders.enc;

import org.apereo.cas.support.saml.services.idp.metadata.SamlRegisteredServiceServiceProviderMetadataFacade;
import org.apereo.cas.ticket.SamlArtifactTicket;
import org.apereo.cas.ticket.SamlArtifactTicketFactory;
import org.apereo.cas.ticket.TicketGrantingTicket;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.web.support.CookieRetrievingCookieGenerator;
import org.apereo.cas.web.support.CookieUtils;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.saml.common.binding.artifact.impl.BasicSAMLArtifactMap;
import org.opensaml.saml.common.messaging.context.SAMLArtifactContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.artifact.SAML2ArtifactType0004;
import org.opensaml.saml.saml2.binding.encoding.impl.BaseSAML2MessageEncoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPArtifactEncoder;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.metadata.AssertionConsumerService;
import org.springframework.ui.velocity.VelocityEngineFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;

/**
 * This is {@link SamlResponseArtifactEncoder}.
 *
 * @author Misagh Moayyed
 * @since 5.2.0
 */
public class SamlResponseArtifactEncoder extends BaseSamlResponseEncoder {
    private final TicketRegistry ticketRegistry;
    private final SamlArtifactTicketFactory samlArtifactTicketFactory;
    private final AuthnRequest authnRequest;
    private final CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator;

    public SamlResponseArtifactEncoder(final VelocityEngineFactory velocityEngineFactory,
                                       final SamlRegisteredServiceServiceProviderMetadataFacade adaptor,
                                       final HttpServletRequest httpRequest,
                                       final HttpServletResponse httpResponse, final AuthnRequest authnRequest,
                                       final TicketRegistry ticketRegistry,
                                       final SamlArtifactTicketFactory samlArtifactTicketFactory,
                                       final CookieRetrievingCookieGenerator ticketGrantingTicketCookieGenerator) {
        super(velocityEngineFactory, adaptor, httpResponse, httpRequest);
        this.ticketRegistry = ticketRegistry;
        this.samlArtifactTicketFactory = samlArtifactTicketFactory;
        this.authnRequest = authnRequest;
        this.ticketGrantingTicketCookieGenerator = ticketGrantingTicketCookieGenerator;
    }

    @Override
    protected String getBinding() {
        return SAMLConstants.SAML2_ARTIFACT_BINDING_URI;
    }

    @Override
    protected BaseSAML2MessageEncoder getMessageEncoderInstance() throws Exception {
        final HTTPArtifactEncoder encoder = new HTTPArtifactEncoder();
        encoder.setVelocityEngine(this.velocityEngineFactory.createVelocityEngine());
        return encoder;
    }
    

    @Override
    protected void finalizeEncode(final BaseSAML2MessageEncoder e,
                                  final Response samlResponse,
                                  final String relayState) throws Exception {

        final TicketGrantingTicket ticketGrantingTicket = CookieUtils.getTicketGrantingTicketFromRequest(
                ticketGrantingTicketCookieGenerator, this.ticketRegistry, this.httpRequest);
        final SamlArtifactTicket ticket = samlArtifactTicketFactory.create(ticketGrantingTicket.getAuthentication(),
                ticketGrantingTicket,
                samlResponse.getIssuer().getValue(),
                adaptor.getEntityId(), samlResponse);
        this.ticketRegistry.addTicket(ticket);

        final BasicSAMLArtifactMap map = new BasicSAMLArtifactMap();
        map.initialize();

        map.setArtifactLifetime(TimeUnit.SECONDS.toMillis(ticket.getExpirationPolicy().getTimeToLive()));
        map.put(ticket.getId(), ticket.getRelyingPartyId(), ticket.getIssuer(), samlResponse);

        final HTTPArtifactEncoder encoder = (HTTPArtifactEncoder) e;
        encoder.setArtifactMap(map);
        
        final MessageContext ctx = getEncoderMessageContext(samlResponse, relayState);
        final SAMLArtifactContext art = ctx.getSubcontext(SAMLArtifactContext.class, true);
        art.setArtifactType(SAML2ArtifactType0004.TYPE_CODE);
        art.setSourceEntityId(samlResponse.getIssuer().getValue());
        art.setSourceArtifactResolutionServiceEndpointIndex(1);
        final AssertionConsumerService svc = adaptor.getAssertionConsumerServiceForArtifactBinding();
        art.setSourceArtifactResolutionServiceEndpointURL(svc.getLocation());
        
        encoder.setMessageContext(ctx);
        
        super.finalizeEncode(e, samlResponse, relayState);
    }
}
