/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * NOTE: This file has been modified by Sony Mobile Communications Inc.
 * Modifications are licensed under the License.
 ******************************************************************************/

package com.gsma.rcs.core.ims.service.sip.messaging;

import com.gsma.rcs.core.ims.protocol.sip.SipException;
import com.gsma.rcs.core.ims.protocol.sip.SipRequest;
import com.gsma.rcs.core.ims.protocol.sip.SipResponse;
import com.gsma.rcs.core.ims.service.ImsService;
import com.gsma.rcs.core.ims.service.ImsSessionListener;
import com.gsma.rcs.core.ims.service.sip.SipSessionError;
import com.gsma.rcs.core.ims.service.sip.SipSessionListener;
import com.gsma.rcs.provider.contact.ContactManager;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.rcs.service.api.ServerApiUtils;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import javax2.sip.InvalidArgumentException;

/**
 * Originating SIP MSRP session
 * 
 * @author jexa7410
 */
public class OriginatingSipMsrpSession extends GenericSipMsrpSession {
    /**
     * The logger
     */
    private static final Logger sLogger = Logger.getLogger(OriginatingSipMsrpSession.class
            .getSimpleName());

    /**
     * Constructor
     * 
     * @param parent IMS service
     * @param contact Remote contact Id
     * @param featureTag Feature tag
     * @param rcsSettings
     * @param timestamp Local timestamp for the session
     * @param contactManager
     * @param serverApiUtils
     */
    public OriginatingSipMsrpSession(ImsService parent, ContactId contact, String featureTag,
            RcsSettings rcsSettings, long timestamp, ContactManager contactManager, ServerApiUtils serverApiUtils) {
        super(parent, contact, featureTag, rcsSettings, timestamp, contactManager, serverApiUtils);

        // Create dialog path
        createOriginatingDialogPath();
    }

    /**
     * Background processing
     */
    public void run() {
        try {
            if (sLogger.isActivated()) {
                sLogger.info("Initiate a new MSRP session as originating");
            }

            // Set setup mode
            String localSetup = createMobileToMobileSetupOffer();
            if (sLogger.isActivated()) {
                sLogger.debug("Local setup attribute is ".concat(localSetup));
            }

            // Build SDP offer
            String sdp = generateSdp(localSetup);

            // Set the local SDP part in the dialog path
            getDialogPath().setLocalContent(sdp);

            // Create an INVITE request
            if (sLogger.isActivated()) {
                sLogger.info("Send INVITE");
            }
            SipRequest invite = createInvite();

            // Set the Authorization header
            getAuthenticationAgent().setAuthorizationHeader(invite);

            // Set initial request in the dialog path
            getDialogPath().setInvite(invite);

            // Send INVITE request
            sendInvite(invite);
        } catch (SipException e) {
            sLogger.error(
                    new StringBuilder("Session initiation has failed for CallId=")
                            .append(getDialogPath().getCallId()).append(" ContactId=")
                            .append(getRemoteContact()).toString(), e);
            handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED, e));
        } catch (InvalidArgumentException e) {
            sLogger.error(
                    new StringBuilder("Session initiation has failed for CallId=")
                            .append(getDialogPath().getCallId()).append(" ContactId=")
                            .append(getRemoteContact()).toString(), e);
            handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED, e));
        } catch (RuntimeException e) {
            /**
             * Intentionally catch runtime exceptions as else it will abruptly end the thread and
             * eventually bring the whole system down, which is not intended.
             */
            sLogger.error(
                    new StringBuilder("Session initiation has failed for CallId=")
                            .append(getDialogPath().getCallId()).append(" ContactId=")
                            .append(getRemoteContact()).toString(), e);
            handleError(new SipSessionError(SipSessionError.SESSION_INITIATION_FAILED, e));
        }
    }

    @Override
    public boolean isInitiatedByRemote() {
        return false;
    }

    @Override
    public void handle180Ringing(SipResponse response) {
        if (sLogger.isActivated()) {
            sLogger.debug("handle180Ringing");
        }
        ContactId contact = getRemoteContact();
        for (ImsSessionListener listener : getListeners()) {
            ((SipSessionListener) listener).handle180Ringing(contact);
        }
    }
}
