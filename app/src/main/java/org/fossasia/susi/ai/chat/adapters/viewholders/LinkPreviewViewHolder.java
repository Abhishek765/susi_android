package org.fossasia.susi.ai.chat.adapters.viewholders;

import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.customtabs.CustomTabsIntent;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.fossasia.susi.ai.R;
import org.fossasia.susi.ai.chat.ParseSusiResponseHelper;
import org.fossasia.susi.ai.chat.adapters.recycleradapters.ChatFeedRecyclerAdapter;
import org.fossasia.susi.ai.data.model.ChatMessage;
import org.fossasia.susi.ai.data.model.WebLink;
import org.fossasia.susi.ai.dataclasses.SkillRatingQuery;
import org.fossasia.susi.ai.helper.Constant;
import org.fossasia.susi.ai.helper.PrefManager;
import org.fossasia.susi.ai.rest.ClientBuilder;
import org.fossasia.susi.ai.rest.responses.susi.SkillRatingResponse;

import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.github.ponnamkarthik.richlinkpreview.MetaData;
import io.github.ponnamkarthik.richlinkpreview.ResponseListener;
import io.github.ponnamkarthik.richlinkpreview.RichPreview;
import io.realm.Realm;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

import static android.os.SystemClock.sleep;
import static org.fossasia.susi.ai.chat.adapters.recycleradapters.ChatFeedRecyclerAdapter.USER_WITHLINK;

/**
 * <h1>Link preview view holder</h1>
 * <p>
 * Created by better_clever on 12/10/16.
 */
public class LinkPreviewViewHolder extends MessageViewHolder {

    @BindView(R.id.text)
    public TextView text;
    @BindView(R.id.background_layout)
    public LinearLayout backgroundLayout;
    @BindView(R.id.link_preview_image)
    public ImageView previewImageView;
    @BindView(R.id.link_preview_title)
    public TextView titleTextView;
    @BindView(R.id.link_preview_description)
    public TextView descriptionTextView;
    @BindView(R.id.timestamp)
    public TextView timestampTextView;
    @BindView(R.id.preview_layout)
    public LinearLayout previewLayout;
    @Nullable
    @BindView(R.id.received_tick)
    public ImageView receivedTick;
    @Nullable
    @BindView(R.id.thumbs_up)
    protected ImageView thumbsUp;
    @Nullable
    @BindView(R.id.thumbs_down)
    protected ImageView thumbsDown;

    private Realm realm;
    private String url;
    private ChatMessage model;

    /**
     * Instantiates a new Link preview view holder.
     *
     * @param itemView the item view
     * @param listener the listener
     */
    public LinkPreviewViewHolder(View itemView, ClickListener listener) {
        super(itemView, listener);
        realm = Realm.getDefaultInstance();
        ButterKnife.bind(this, itemView);
    }

    /**
     * Inflate Link Preview
     *
     * @param model       the ChatMessage object
     * @param currContext the Context
     */
    public void setView(final ChatMessage model, int viewType, final Context currContext) {
        this.model = model;
        Spanned answerText;
        text.setLinksClickable(true);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            answerText = Html.fromHtml(model.getContent(), Html.FROM_HTML_MODE_COMPACT);
        } else {
            answerText = Html.fromHtml(model.getContent());
        }

        if (viewType == USER_WITHLINK) {
            if (model.getIsDelivered()) {
                if (receivedTick != null) {
                    receivedTick.setImageResource(R.drawable.ic_check);
                }
            } else {
                if (receivedTick != null) {
                    receivedTick.setImageResource(R.drawable.ic_clock);
                }
            }
        }

        if (viewType != USER_WITHLINK) {
            if (model.getSkillLocation().isEmpty()) {
                if (thumbsUp != null) {
                    thumbsUp.setVisibility(View.GONE);
                }
                if (thumbsDown != null) {
                    thumbsDown.setVisibility(View.GONE);
                }
            } else {
                if (thumbsUp != null) {
                    thumbsUp.setVisibility(View.VISIBLE);
                }
                if (thumbsDown != null) {
                    thumbsDown.setVisibility(View.VISIBLE);
                }
            }

            if (model.isPositiveRated() || model.isNegativeRated()) {
                thumbsUp.setVisibility(View.GONE);
                thumbsDown.setVisibility(View.GONE);
            } else {
                thumbsUp.setImageResource(R.drawable.thumbs_up_outline);
                thumbsDown.setImageResource(R.drawable.thumbs_down_outline);
            }

            thumbsUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!model.isPositiveRated() && !model.isNegativeRated()) {
                        thumbsUp.setImageResource(R.drawable.thumbs_up_solid);
                        rateSusiSkill(Constant.POSITIVE, model.getSkillLocation(), currContext);
                        setRating(true, true);
                    }
                }
            });

            thumbsDown.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!model.isPositiveRated() && !model.isNegativeRated()) {
                        thumbsDown.setImageResource(R.drawable.thumbs_down_solid);
                        rateSusiSkill(Constant.NEGATIVE, model.getSkillLocation(), currContext);
                        setRating(true, false);
                    }
                }
            });

        }

        text.setText(answerText);
        timestampTextView.setText(model.getTimeStamp());
        if (model.getWebLinkData() == null) {
            previewImageView.setVisibility(View.GONE);
            descriptionTextView.setVisibility(View.GONE);
            titleTextView.setVisibility(View.GONE);
            previewLayout.setVisibility(View.GONE);

            RichPreview richPreview = new RichPreview(getResponseListener());

            List<String> urlList = ChatFeedRecyclerAdapter.extractLinks(model.getContent());
            String url = urlList.get(0);
            String http = "http://";
            String https = "https://";
            if (!(url.startsWith(http) || url.startsWith(https))) {
                url = https + url;
            }

            richPreview.getPreview(url);
        } else {

            if (!model.getWebLinkData().getHeadline().isEmpty()) {
                Timber.d("onPos: %s", model.getWebLinkData().getHeadline());
                titleTextView.setText(model.getWebLinkData().getHeadline());
            } else {
                titleTextView.setVisibility(View.GONE);
                Timber.d("handleItemEvents: isEmpty");
            }

            if (!model.getWebLinkData().getBody().isEmpty()) {
                Timber.d("onPos: %s", model.getWebLinkData().getHeadline());
                descriptionTextView.setText(model.getWebLinkData().getBody());
            } else {
                descriptionTextView.setVisibility(View.GONE);
                Timber.d("handleItemEvents: isEmpty");
            }

            if (model.getWebLinkData().getHeadline().isEmpty() && model.getWebLinkData().getBody().isEmpty()) {
                previewLayout.setVisibility(View.GONE);
            }

            Timber.i(model.getWebLinkData().getImageURL());
            if (!model.getWebLinkData().getImageURL().equals("")) {
                Picasso.with(currContext.getApplicationContext()).load(model.getWebLinkData().getImageURL())
                        .fit().centerCrop()
                        .into(previewImageView);
            } else {
                previewImageView.setVisibility(View.GONE);
            }

            url = model.getWebLinkData().getUrl();
        }

        /*
          Redirects to the link through chrome custom tabs
         */

        previewLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri webpage = Uri.parse(url);
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(); //custom tabs intent builder
                CustomTabsIntent customTabsIntent = builder.build();
                customTabsIntent.launchUrl(currContext, webpage); //launching through custom tabs
            }
        });
    }

    private void setRating(boolean what, boolean which) {
        Realm realm = Realm.getDefaultInstance();
        realm.beginTransaction();
        if (which) {
            model.setPositiveRated(what);
        } else {
            model.setNegativeRated(what);
        }
        realm.commitTransaction();
    }

    private void rateSusiSkill(final String polarity, String locationUrl, final Context context) {

        final Map<String, String> susiLocation = ParseSusiResponseHelper.Companion.getSkillLocation(locationUrl);

        if (susiLocation.size() == 0)
            return;

        SkillRatingQuery queryObject = new SkillRatingQuery(susiLocation.get("model"), susiLocation.get("group"),
                susiLocation.get("language"), susiLocation.get("skill"), polarity);

        Call<SkillRatingResponse> call = ClientBuilder.rateSkillCall(queryObject);

        call.enqueue(new Callback<SkillRatingResponse>() {
            @Override
            public void onResponse(Call<SkillRatingResponse> call, Response<SkillRatingResponse> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    switch (polarity) {
                        case Constant.POSITIVE:
                            if (thumbsUp != null) {
                                thumbsUp.setImageResource(R.drawable.thumbs_up_outline);
                                setRating(false, true);
                            }
                            break;
                        case Constant.NEGATIVE:
                            if (thumbsDown != null) {
                                thumbsDown.setImageResource(R.drawable.thumbs_down_outline);
                                setRating(false, false);
                            }
                            break;
                    }
                    Toast.makeText(context, context.getString(R.string.error_rating), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<SkillRatingResponse> call, Throwable t) {
                Timber.e(t);
                switch (polarity) {
                    case Constant.POSITIVE:
                        if (thumbsUp != null) {
                            thumbsUp.setImageResource(R.drawable.thumbs_up_outline);
                            setRating(false, true);
                        }
                        break;
                    case Constant.NEGATIVE:
                        if (thumbsDown != null) {
                            thumbsDown.setImageResource(R.drawable.thumbs_down_outline);
                            setRating(false, false);
                        }
                        break;
                }
                Toast.makeText(context, context.getString(R.string.error_rating), Toast.LENGTH_SHORT).show();
            }

        });
    }

    private ResponseListener getResponseListener() {
        return new ResponseListener() {
            @Override
            public void onData(MetaData data) {
                if (!PrefManager.hasTokenExpired() || PrefManager.getBoolean(Constant.ANONYMOUS_LOGGED_IN, false)) {
                    realm.beginTransaction();
                    Realm realm = Realm.getDefaultInstance();
                    WebLink link = realm.createObject(WebLink.class);

                    if (data != null) {

                        if (!TextUtils.isEmpty(data.getDescription())) {
                            Timber.d("onPos: %s", data.getDescription());
                            previewLayout.setVisibility(View.VISIBLE);
                            descriptionTextView.setVisibility(View.VISIBLE);
                            descriptionTextView.setText(data.getDescription());
                        }

                        if (!TextUtils.isEmpty(data.getTitle())) {
                            Timber.d("onPos: %s", data.getTitle());
                            previewLayout.setVisibility(View.VISIBLE);
                            titleTextView.setVisibility(View.VISIBLE);
                            titleTextView.setText(data.getTitle());
                        }

                        link.setBody(data.getDescription());
                        link.setHeadline(data.getTitle());
                        link.setUrl(data.getUrl());
                        url = data.getUrl();

                        final String imageLink = data.getImageurl();

                        if (TextUtils.isEmpty(imageLink)) {
                            previewImageView.setVisibility(View.GONE);
                            link.setImageURL("");
                        } else {
                            previewImageView.setVisibility(View.VISIBLE);
                            Picasso.with(itemView.getContext()).
                                    load(imageLink)
                                    .fit()
                                    .centerCrop()
                                    .into(previewImageView);
                            link.setImageURL(imageLink);
                        }
                    }

                    model.setWebLinkData(link);
                    realm.copyToRealmOrUpdate(model);
                    realm.commitTransaction();
                }
            }

            @Override
            public void onError(Exception e) {
                Timber.e(e);
            }
        };
    }
}
