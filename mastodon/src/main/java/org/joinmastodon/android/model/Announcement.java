package org.joinmastodon.android.model;

import org.joinmastodon.android.api.ObjectValidationException;
import org.joinmastodon.android.api.RequiredField;
import org.parceler.Parcel;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Parcel
public class Announcement extends BaseModel implements DisplayItemsParent {
    @RequiredField
    public String id;
    @RequiredField
    public String content;
    public Instant startsAt;
    public Instant endsAt;
    public boolean published;
    public boolean allDay;
    public Instant publishedAt;
    public Instant updatedAt;
    public boolean read;
    public List<Emoji> emojis;
	public List<EmojiReaction> reactions;
    public List<Mention> mentions;
    public List<Hashtag> tags;

    @Override
    public String toString() {
        return "Announcement{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", startsAt=" + startsAt +
                ", endsAt=" + endsAt +
                ", published=" + published +
                ", allDay=" + allDay +
                ", publishedAt=" + publishedAt +
                ", updatedAt=" + updatedAt +
                ", read=" + read +
                ", emojis=" + emojis +
                ", mentions=" + mentions +
                ", tags=" + tags +
                '}';
    }

	@Override
	public void postprocess() throws ObjectValidationException{
		super.postprocess();
		if(reactions==null) reactions=new ArrayList<>();
	}

	public Status toStatus(boolean isIceshrimp) {
        Status s=Status.ofFake(id, content, publishedAt);
		s.createdAt=startsAt != null ? startsAt : publishedAt;
		s.reactions=reactions;
        if(updatedAt != null && (!isIceshrimp || !updatedAt.equals(publishedAt))) s.editedAt=updatedAt;
        return s;
    }

    @Override
    public String getID() {
        return id;
    }
}
