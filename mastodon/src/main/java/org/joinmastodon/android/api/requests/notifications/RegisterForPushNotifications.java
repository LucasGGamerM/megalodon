package org.joinmastodon.android.api.requests.notifications;

import org.joinmastodon.android.api.MastodonAPIRequest;
import org.joinmastodon.android.model.PushSubscription;

public class RegisterForPushNotifications extends MastodonAPIRequest<PushSubscription>{
	public RegisterForPushNotifications(String endpoint, Boolean standard, String encryptionKey, String authKey, PushSubscription.Alerts alerts, PushSubscription.Policy policy){
		super(HttpMethod.POST, "/push/subscription", PushSubscription.class);
		Request r=new Request();
		r.subscription.endpoint=endpoint;
		r.subscription.standard = standard;
		r.data.alerts=alerts;
		r.policy=policy;
		r.subscription.keys.p256dh=encryptionKey;
		r.subscription.keys.auth=authKey;
		setRequestBody(r);
	}

	private static class Request{
		public Subscription subscription=new Subscription();
		public Data data=new Data();
		public PushSubscription.Policy policy;

		private static class Keys{
			public String p256dh;
			public String auth;
		}

		private static class Subscription{
			public String endpoint;
			// Use standard push notifications if available
			public Boolean standard;
			public Keys keys=new Keys();
		}

		private static class Data{
			public PushSubscription.Alerts alerts;
		}
	}
}
