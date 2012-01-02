/*
 * Copyright (c) 2011, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.apptentive.android.sdk;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import com.apptentive.android.sdk.module.metric.MetricPayload;
import com.apptentive.android.sdk.offline.FeedbackPayload;
import com.apptentive.android.sdk.offline.PayloadManager;
import com.apptentive.android.sdk.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * This module is responsible for showing the feedback dialog, and sending feedback payloads to the Apptentive server.
 * @author Sky Kelsey
 */
public class FeedbackModule {

	// *************************************************************************************************
	// ********************************************* Static ********************************************
	// *************************************************************************************************

	private static FeedbackModule instance = null;

	static FeedbackModule getInstance() {
		if (instance == null) {
			instance = new FeedbackModule();
		}
		return instance;
	}


	// *************************************************************************************************
	// ********************************************* Private *******************************************
	// *************************************************************************************************

	private SharedPreferences prefs;
	private FeedbackPayload feedback;
	private Map<String, String> dataFields;
	private String startingEmail;

	private FeedbackModule() {
		dataFields = new HashMap<String, String>();
		feedback = new FeedbackPayload("feedback");
	}


	private void submit() {
		// Add in the key.value pairs that the developer passed in as "record[data][KEY] = VALUE"
		if (dataFields != null) {
			for (String key : dataFields.keySet()) {
				try {
					feedback.setString(dataFields.get(key), "record", "data", key);
				} catch (Exception e) {
					Log.e("Error setting developer defined custom feedback field", e);
				}
			}
		}
		// If the email was changed, then save it for future use.
		if(!startingEmail.equals(feedback.getEmail())){
			prefs.edit().putString("userEnteredEmail", feedback.getEmail()).commit();
		}
		PayloadManager.getInstance().putPayload(feedback);
	}


	// *************************************************************************************************
	// ******************************************* Not Private *****************************************
	// *************************************************************************************************

	void setContext(Context context) {
		this.prefs = context.getSharedPreferences("APPTENTIVE", Context.MODE_PRIVATE);
	}

	void showFeedbackDialog(Context context, Trigger reason) {
		new FeedbackDialog(context).show(reason);
	}

	/**
	 * Shows the feedback dialog.
	 * @param activity The activity from which this method was called.
	 */
	public void forceShowFeedbackDialog(Activity activity) {
		showFeedbackDialog(activity, Trigger.forced);
	}

	/**
	 * Adds a data field to subsequent feedback payloads.
	 * @param key The name of the data to send.
	 * @param value The value of the data to send.
	 */
	public void addDataField(String key, String value){
		dataFields.put(key, value);
	}


	// *************************************************************************************************
	// ***************************************** Inner Classes *****************************************
	// *************************************************************************************************

	enum Trigger {
		rating,
		forced
	}


	private final class FeedbackDialog extends Dialog {

		private Context context;

		public FeedbackDialog(Context context) {
			super(context, android.R.style.Theme_Translucent_NoTitleBar);
			this.context = context;
		}

		void show(FeedbackModule.Trigger reason) {
			// Load the use entered email, if it exists. Otherwise, load the default email.
			startingEmail = prefs.getString("userEnteredEmail", null);
			if(startingEmail == null){
				startingEmail = GlobalInfo.userEmail;
			}

			setContentView(R.layout.apptentive_feedback);

			EditText feedback = (EditText) findViewById(R.id.apptentive_feedback_text);
			feedback.addTextChangedListener(new GenericTextWatcher(feedback));
			switch (reason) {
				case forced:
					feedback.setHint(R.string.apptentive_edittext_feedback_message_forced);
					break;
				case rating:
					feedback.setHint(R.string.apptentive_edittext_feedback_message);
					break;
				default:
					break;
			}
			EditText email = (EditText) findViewById(R.id.apptentive_feedback_user_email);
			FeedbackModule.this.feedback.setEmail(startingEmail);
			email.setText(startingEmail);
			email.addTextChangedListener(new GenericTextWatcher(email));

			findViewById(R.id.apptentive_button_cancel).setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					MetricPayload metric = new MetricPayload(MetricPayload.Event.feedback_dialog__cancel);
					PayloadManager.getInstance().putPayload(metric);
					dismiss();
				}
			});

			findViewById(R.id.apptentive_button_send).setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					FeedbackModule.this.submit();
					dismiss();
				}
			});

			findViewById(R.id.apptentive_branding_view).setOnClickListener(new View.OnClickListener() {
				public void onClick(View view) {
					AboutModule.getInstance().show(context);
				}
			});

			// Instrumentation
			MetricPayload metric = new MetricPayload(MetricPayload.Event.feedback_dialog__launch);
			metric.putData("trigger", reason.name());
			PayloadManager.getInstance().putPayload(metric);

			super.show();
		}

		private final class GenericTextWatcher implements TextWatcher {

			private View view;

			private GenericTextWatcher(View view) {
				this.view = view;
			}

			public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
			}

			public void afterTextChanged(Editable editable) {
				String text = editable.toString();
				int id = view.getId();
				if (id == R.id.apptentive_feedback_user_email) {
					FeedbackModule.this.feedback.setEmail(text);
				} else if (id == R.id.apptentive_feedback_text) {
					FeedbackModule.this.feedback.setFeedback(text);
				}
			}
		}
	}
}
