package com.gsma.services.rcs.sharing.video;

import com.gsma.services.rcs.contacts.ContactId;

/**
 * Callback methods for video sharing events
 */
interface IVideoSharingListener {

	void onStateChanged(in ContactId contact, in String sharingId, in int state, in int reasonCode);
}