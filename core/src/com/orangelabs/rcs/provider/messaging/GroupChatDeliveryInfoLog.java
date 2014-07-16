/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
 * Copyright (C) 2014 Sony Mobile Communications Inc.
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

package com.orangelabs.rcs.provider.messaging;

import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_ERROR;
import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.ReasonCode.DISPLAY_ERROR;
import static com.gsma.services.rcs.chat.ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Pair;

import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.orangelabs.rcs.utils.logger.Logger;

/**
 * Class to interface the deliveryinfo table
 * 
 */
public class GroupChatDeliveryInfoLog implements IGroupChatDeliveryInfoLog {

	/**
	 * Delivery info database URI
	 */
	private Uri GroupChatDeliveryInfoDatabaseUri = GroupChatDeliveryInfoData.CONTENT_URI;

	private static final String SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT = new StringBuilder(
			GroupChatDeliveryInfoData.KEY_MSG_ID).append("=? AND ").append(GroupChatDeliveryInfoData.KEY_CONTACT).append("=?")
			.toString();

	private static final String SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE = new StringBuilder(
			GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
			.append(ChatLog.GroupChatDeliveryInfo.DeliveryStatus.NOT_DELIVERED).append(" OR (")
			.append(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS).append("=")
			.append(ChatLog.GroupChatDeliveryInfo.DeliveryStatus.FAILED).append(" AND ")
			.append(GroupChatDeliveryInfoData.KEY_REASON_CODE).append(" IN (")
			.append(ChatLog.GroupChatDeliveryInfo.ReasonCode.DELIVERY_ERROR).append(",")
			.append(ChatLog.GroupChatDeliveryInfo.ReasonCode.DISPLAY_ERROR).append("))").toString();

	/**
	 * Content resolver
	 */
	private ContentResolver cr;

	/**
	 * The logger
	 */
	private static final Logger logger = Logger.getLogger(GroupChatDeliveryInfoLog.class.getSimpleName());

	/**
	 * Constructor
	 * 
	 * @param cr
	 *            Content resolver
	 */
	/* package private */GroupChatDeliveryInfoLog(ContentResolver cr) {
		this.cr = cr;
	}

	@Override
	public Uri addGroupChatDeliveryInfoEntry(String chatId, String msgId, ContactId contact) {
		if (logger.isActivated()) {
			logger.debug("Add new entry: chatID=" + chatId + ", messageID=" + msgId );
		}
		if (logger.isActivated()) {
			logger.debug("Add new entry: chatID=" + chatId + ", messageID=" + msgId + ", contact=" + contact);
		}
		ContentValues values = new ContentValues();
		values.put(GroupChatDeliveryInfoData.KEY_CHAT_ID, chatId);
		values.put(GroupChatDeliveryInfoData.KEY_MSG_ID, msgId);
		if (contact != null) {
			values.put(GroupChatDeliveryInfoData.KEY_CONTACT, contact.toString());
		}
		values.put(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS, ChatLog.GroupChatDeliveryInfo.DeliveryStatus.NOT_DELIVERED);
		values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE, ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, 0);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DISPLAYED, 0);
		return cr.insert(GroupChatDeliveryInfoDatabaseUri, values);
	}

	/**
	 * Get status for individual contact
	 * 
	 * @param msgID
	 *            Message ID
	 * @param contact
	 *            Contact ID for which the status should be retrieved
	 * @return int Status
	 */
	public Pair<Integer, Integer> getGroupChatDeliveryInfoStatus(String msgId, ContactId contact) {
		Cursor cursor = null;
		try {
			String[] selectionArgs = new String[] { msgId, contact.toString() };
			String[] projection = new String[] { GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS,
					GroupChatDeliveryInfoData.KEY_REASON_CODE, };
			cursor = cr.query(GroupChatDeliveryInfoDatabaseUri, projection, SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT,
					selectionArgs, null);
			if (!cursor.moveToFirst()) {
				if (logger.isActivated()) {
					logger.warn("There was no group chat delivery info for msgId '" + msgId + "' and contact '" + contact
							+ "' to get status from!");
					return new Pair<Integer, Integer>(ChatLog.GroupChatDeliveryInfo.DeliveryStatus.NOT_DELIVERED,
							ChatLog.GroupChatDeliveryInfo.ReasonCode.NONE);
				}
			}
			int deliveryStatus = cursor.getInt(cursor.getColumnIndexOrThrow(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS));
			int statusReason = cursor.getInt(cursor.getColumnIndexOrThrow(GroupChatDeliveryInfoData.KEY_REASON_CODE));
			return new Pair<Integer, Integer>(deliveryStatus, statusReason);
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/**
	 * Set delivery status for outgoing group chat messages and files
	 * 
	 * @param msgID
	 *            Message ID
	 * @param deliveryStatus
	 *            Delivery status of entry
	 * @param reasonCode
	 *            Reason code for status
	 * @param contact
	 *            The contact ID for which the entry is to be updated
	 */
	public void updateGroupChatDeliveryInfoStatus(String msgId, int deliveryStatus, int reasonCode, ContactId contact) {
		ContentValues values = new ContentValues();
		values.put(GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS, deliveryStatus);
		values.put(GroupChatDeliveryInfoData.KEY_TIMESTAMP_DELIVERED, System.currentTimeMillis());
		values.put(GroupChatDeliveryInfoData.KEY_REASON_CODE, reasonCode);
		String[] selectionArgs = new String[] { msgId, contact.toString() };
		if (cr.update(GroupChatDeliveryInfoDatabaseUri, values, SELECTION_DELIVERY_INFO_BY_MSG_ID_AND_CONTACT, selectionArgs) < 1) {
			/* TODO: Throw exception */
			if (logger.isActivated()) {
				logger.warn("There was not group chat delivery into for msgId '" + msgId + "' and contact '" + contact
						+ "' to update!");
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDeliveredToAllRecipients(java.lang.String)
	 */
	@Override
	public boolean isDeliveredToAllRecipients(String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null,
					SELECTION_CONTACTS_NOT_RECEIVED_MESSAGE, null, null);
			return !cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.orangelabs.rcs.provider.messaging.IGroupChatDeliveryInfoLog#isDisplayedByAllRecipients(java.lang.String)
	 */
	@Override
	public boolean isDisplayedByAllRecipients(String msgId) {
		Cursor cursor = null;
		try {
			cursor = cr.query(Uri.withAppendedPath(GroupChatDeliveryInfoData.CONTENT_MSG_URI, msgId), null,
					GroupChatDeliveryInfoData.KEY_DELIVERY_STATUS + "!= " + ChatLog.GroupChatDeliveryInfo.DeliveryStatus.DISPLAYED,
					null, null);
			return !cursor.moveToFirst();
		} finally {
			if (cursor != null) {
				cursor.close();
			}
		}
	}
}