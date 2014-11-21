/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010 France Telecom S.A.
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
 ******************************************************************************/

package com.orangelabs.rcs.ri.messaging.chat;

import java.util.ArrayList;
import java.util.Set;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.gsma.services.rcs.RcsServiceException;
import com.gsma.services.rcs.RcsServiceNotAvailableException;
import com.gsma.services.rcs.chat.ChatLog;
import com.gsma.services.rcs.chat.ChatMessage;
import com.gsma.services.rcs.chat.ChatService;
import com.gsma.services.rcs.chat.ChatServiceConfiguration;
import com.gsma.services.rcs.chat.Geoloc;
import com.gsma.services.rcs.chat.GeolocMessage;
import com.gsma.services.rcs.contacts.ContactId;
import com.orangelabs.rcs.ri.ApiConnectionManager;
import com.orangelabs.rcs.ri.ApiConnectionManager.RcsServiceName;
import com.orangelabs.rcs.ri.R;
import com.orangelabs.rcs.ri.messaging.geoloc.EditGeoloc;
import com.orangelabs.rcs.ri.messaging.geoloc.ShowUsInMap;
import com.orangelabs.rcs.ri.utils.LockAccess;
import com.orangelabs.rcs.ri.utils.LogUtils;
import com.orangelabs.rcs.ri.utils.RcsDisplayName;
import com.orangelabs.rcs.ri.utils.Utils;

/**
 * Chat view
 */
public abstract class ChatView extends FragmentActivity implements LoaderManager.LoaderCallbacks<Cursor>, IChatView {
	/**
	 * The loader's unique ID. Loader IDs are specific to the Activity in which they reside.
	 */
	protected static final int LOADER_ID = 1;

	/**
	 * Activity result constant
	 */
	private final static int SELECT_GEOLOCATION = 0;

	/**
	 * The adapter that binds data to the ListView
	 */
	protected ChatCursorAdapter mAdapter;

	/**
	 * Message composer
	 */
	protected EditText composeText;

	/**
	 * Utility class to manage the is-composing status
	 */
	protected IsComposingManager composingManager;

	/**
	 * A locker to exit only once
	 */
	protected LockAccess exitOnce = new LockAccess();

	/**
	 * Activity displayed status
	 */
	private static boolean activityDisplayed = false;

	/**
	 * API connection manager
	 */
	protected ApiConnectionManager connectionManager;

	/**
	 * UI handler
	 */
	protected Handler handler = new Handler();

	protected ListView listView;

	/**
	 * The log tag for this class
	 */
	private static final String LOGTAG = LogUtils.getTag(ChatView.class.getSimpleName());

	/** 
	 * MESSAGE_ID is the ID since it is unique
	 */
	private static final String MESSAGE_ID_AS_ID = new StringBuilder(ChatLog.Message.MESSAGE_ID).append(" AS ").append(BaseColumns._ID)
			.toString();

	protected static final String[] PROJECTION = new String[] { MESSAGE_ID_AS_ID, ChatLog.Message.MIME_TYPE,
			ChatLog.Message.CONTENT, ChatLog.Message.TIMESTAMP, ChatLog.Message.MESSAGE_STATUS, ChatLog.Message.DIRECTION,
			ChatLog.Message.CONTACT };

	protected final static String QUERY_SORT_ORDER = new StringBuilder(ChatLog.Message.TIMESTAMP).append(" ASC").toString();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// Set layout
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		setContentView(R.layout.chat_view);

		// Set message composer callbacks
		composeText = (EditText) findViewById(R.id.userText);
		composeText.setOnKeyListener(new OnKeyListener() {

			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					switch (keyCode) {
					case KeyEvent.KEYCODE_DPAD_CENTER:
					case KeyEvent.KEYCODE_ENTER:
						sendText();
						return true;
					}
				}
				return false;
			}
		});

		composeText.addTextChangedListener(new TextWatcher() {

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				// Check if the text is not null.
				// we do not wish to consider putting the edit text back to null (like when sending message), is having activity
				if (!TextUtils.isEmpty(s)) {
					// Warn the composing manager that we have some activity
					if (composingManager != null) {
						composingManager.hasActivity();
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});

		// Set send button listener
		Button sendBtn = (Button) findViewById(R.id.send_button);
		sendBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendText();
			}
		});

		// Initialize the adapter.
		mAdapter = new ChatCursorAdapter(this, null, 0, isSingleChat());

		// Associate the list adapter with the ListView.
		listView = (ListView) findViewById(android.R.id.list);
		listView.setAdapter(mAdapter);
		registerForContextMenu(listView);

		// Register to API connection manager
		connectionManager = ApiConnectionManager.getInstance(this);

		if (connectionManager == null || !connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACTS)) {
			Utils.showMessageAndExit(this, getString(R.string.label_service_not_available), exitOnce);
			return;
		}
		connectionManager.startMonitorServices(this, exitOnce, RcsServiceName.CHAT, RcsServiceName.CONTACTS);
		if (!processIntent()) {
			return;
		}
		ChatService chatService = connectionManager.getChatApi();
		try {
			addChatEventListener(chatService);
			ChatServiceConfiguration configuration = chatService.getConfiguration();
			// Set max label length
			int maxMsgLength = configuration.getOneToOneChatMessageMaxLength();
			if (maxMsgLength > 0) {
				// Set the message composer max length
				InputFilter[] filterArray = new InputFilter[1];
				filterArray[0] = new InputFilter.LengthFilter(maxMsgLength);
				composeText.setFilters(filterArray);
			}
			// Instantiate the composing manager
			composingManager = new IsComposingManager(configuration.getIsComposingTimeout() * 1000, getNotifyComposing());
		} catch (RcsServiceNotAvailableException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_disabled), exitOnce);
		} catch (RcsServiceException e) {
			Utils.showMessageAndExit(this, getString(R.string.label_api_failed), exitOnce);
		}
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onCreate");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (connectionManager == null) {
			return;
		}
		connectionManager.stopMonitorServices(this);
		if (connectionManager.isServiceConnected(RcsServiceName.CHAT)) {
			try {
				removeChatEventListener(connectionManager.getChatApi());
			} catch (RcsServiceException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		activityDisplayed = true;
	}

	@Override
	protected void onPause() {
		super.onStart();
		activityDisplayed = false;
	}

	/**
	 * Return true if the activity is currently displayed or not
	 * 
	 * @return Boolean
	 */
	public static boolean isDisplayed() {
		return activityDisplayed;
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onNewIntent");
		}
		super.onNewIntent(intent);
		// Replace the value of intent
		setIntent(intent);

		if (connectionManager.isServiceConnected(RcsServiceName.CHAT, RcsServiceName.CONTACTS)) {
			processIntent();
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onLoadFinished " + loader.getId());
		}
		// A switch-case is useful when dealing with multiple Loaders/IDs
		switch (loader.getId()) {
		case LOADER_ID:
			// The asynchronous load is complete and the data
			// is now available for use. Only now can we associate
			// the queried Cursor with the CursorAdapter.
			mAdapter.swapCursor(cursor);
			break;
		}
		// The listview now displays the queried data.
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		if (LogUtils.isActive) {
			Log.d(LOGTAG, "onLoaderReset " + loader.getId());
		}
		// For whatever reason, the Loader's data is now unavailable.
		// Remove any references to the old data by replacing it with a null Cursor.
		mAdapter.swapCursor(null);
	}

	/**
	 * Send a text and display it
	 */
	private void sendText() {
		String text = composeText.getText().toString();
		if (TextUtils.isEmpty(text)) {
			return;
		}
		// Check if the service is available
		if (!isServiceRegistered()) {
			return;
		}
		// Send text message
		ChatMessage message = sendTextMessage(text);
		if (message != null) {
			// Warn the composing manager that the message was sent
			composingManager.messageWasSent();
			composeText.setText(null);
		} else {
			Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
		}
	}

	/**
	 * Send a geolocation
	 * 
	 * @param geoloc
	 */
	private void sendGeoloc(Geoloc geoloc) {
		// Check if the service is available
		if (!isServiceRegistered()) {
			return;
		}
		// Send text message
		GeolocMessage message = sendGeolocMessage(geoloc);
		if (message == null) {
			Utils.showMessage(ChatView.this, getString(R.string.label_send_im_failed));
		}
	}

	boolean isServiceRegistered() {
		// Check if the service is available
		boolean registered = false;
		try {
			registered = ApiConnectionManager.getInstance(ChatView.this).getChatApi().isServiceRegistered();
		} catch (Exception e) {
			if (LogUtils.isActive) {
				Log.e(LOGTAG, "isRegistered failed", e);
			}
		}
		if (!registered) {
			Utils.showMessage(ChatView.this, getString(R.string.label_service_not_available));
		}
		return registered;
	}

	/**
	 * Add quick text
	 */
	protected void addQuickText() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.label_select_quicktext);
		builder.setCancelable(true);
		builder.setItems(R.array.select_quicktext, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				String[] items = getResources().getStringArray(R.array.select_quicktext);
				composeText.append(items[which]);
			}
		});

		AlertDialog alert = builder.create();
		alert.show();
	}

	/**
	 * Get a geoloc
	 */
	protected void getGeoLoc() {
		// Start a new activity to send a geolocation
		startActivityForResult(new Intent(this, EditGeoloc.class), SELECT_GEOLOCATION);
	}

	/**
	 * On activity result
	 * 
	 * @param requestCode
	 *            Request code
	 * @param resultCode
	 *            Result code
	 * @param data
	 *            Data
	 */
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		switch (requestCode) {
		case SELECT_GEOLOCATION:
			// Get selected geoloc
			Geoloc geoloc = data.getParcelableExtra(EditGeoloc.EXTRA_GEOLOC);
			// Send geoloc
			sendGeoloc(geoloc);
			break;
		}
	}

	/**
	 * Show us in a map
	 * 
	 * @param participants
	 *            Set of participants
	 */
	protected void showUsInMap(Set<String> participants) {
		ShowUsInMap.startShowUsInMap(this, new ArrayList<String>(participants));
	}

	/**
	 * Display composing event for contact
	 * 
	 * @param contact
	 * @param status
	 *            True if contact is composing
	 */
	protected void displayComposingEvent(final ContactId contact, final boolean status) {
		final String from = RcsDisplayName.getInstance(this).getDisplayName(contact);
		// Execute on UI handler since callback is executed from service
		handler.post(new Runnable() {
			public void run() {
				TextView view = (TextView) findViewById(R.id.isComposingText);
				if (status) {
					// Display is-composing notification
					view.setText(getString(R.string.label_contact_is_composing, from));
					view.setVisibility(View.VISIBLE);
				} else {
					// Hide is-composing notification
					view.setVisibility(View.GONE);
				}
			}
		});
	}
}