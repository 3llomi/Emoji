package com.vanniktech.emoji_stickers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.vanniktech.emoji.EmojiManager;
import com.vanniktech.emoji.EmojiPagerAdapter;
import com.vanniktech.emoji.Utils;
import com.vanniktech.emoji.emoji.EmojiCategory;
import com.vanniktech.emoji.listeners.OnEmojiBackspaceClickListener;
import com.vanniktech.emoji.listeners.OnEmojiClickListener;
import com.vanniktech.emoji.listeners.OnEmojiLongClickListener;
import com.vanniktech.emoji.listeners.RepeatListener;
import com.vanniktech.emoji_stickers.stickers.GridAutofitLayoutManager;
import com.vanniktech.emoji_stickers.stickers.StickerAdapter;
import com.vanniktech.emoji_stickers.stickers.StickerSettings;

import static java.util.concurrent.TimeUnit.SECONDS;

@SuppressLint("ViewConstructor")
public final class EmojiViewSticker extends LinearLayout implements ViewPager.OnPageChangeListener {
    private static final long INITIAL_INTERVAL = SECONDS.toMillis(1) / 2;
    private static final int NORMAL_INTERVAL = 50;

    @ColorInt
    private final int themeAccentColor;
    @ColorInt
    private final int themeIconColor;

    private final ImageButton[] emojiTabs;
    private final EmojiPagerAdapter emojiPagerAdapter;

    @Nullable
    OnEmojiBackspaceClickListener onEmojiBackspaceClickListener;

    private int emojiTabLastSelectedIndex = -1;

    @SuppressWarnings("PMD.CyclomaticComplexity")
    public EmojiViewSticker(final Context context,
                            final OnEmojiClickListener onEmojiClickListener,
                            final OnEmojiLongClickListener onEmojiLongClickListener, @NonNull final EmojiPopupSticker.Builder builder) {
        super(context);

        View.inflate(context, R.layout.emoji_view, this);

        setOrientation(VERTICAL);
        setBackgroundColor(builder.backgroundColor != 0 ? builder.backgroundColor : Utils.resolveColor(context, R.attr.emojiBackground, R.color.emoji_background));
        themeIconColor = builder.iconColor != 0 ? builder.iconColor : Utils.resolveColor(context, R.attr.emojiIcons, R.color.emoji_icons);

        final TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorAccent, value, true);
        themeAccentColor = builder.selectedIconColor != 0 ? builder.selectedIconColor : value.data;

        final ViewPager emojisPager = findViewById(R.id.emojiViewPager);
        final View emojiDivider = findViewById(R.id.emojiViewDivider);
        emojiDivider.setBackgroundColor(builder.dividerColor != 0 ? builder.dividerColor : Utils.resolveColor(context, R.attr.emojiDivider, R.color.emoji_divider));

        if (builder.pageTransformer != null) {
            emojisPager.setPageTransformer(true, builder.pageTransformer);
        }

        final LinearLayout emojisTab = findViewById(R.id.emojiViewTab);
        emojisPager.addOnPageChangeListener(this);

        final EmojiCategory[] categories = EmojiManager.getInstance().getCategories();

        emojiTabs = new ImageButton[categories.length + 2];
        emojiTabs[0] = inflateButton(context, R.drawable.emoji_recent, R.string.emoji_category_recent, emojisTab);
        for (int i = 0; i < categories.length; i++) {
            emojiTabs[i + 1] = inflateButton(context, categories[i].getIcon(), categories[i].getCategoryName(), emojisTab);
        }
        emojiTabs[emojiTabs.length - 1] = inflateButton(context, R.drawable.emoji_backspace, R.string.emoji_backspace, emojisTab);

        handleOnClicks(emojisPager);

        emojiPagerAdapter = new EmojiPagerAdapter(onEmojiClickListener, onEmojiLongClickListener, builder.recentEmoji, builder.variantEmoji);
        emojisPager.setAdapter(emojiPagerAdapter);

        final int startIndex = emojiPagerAdapter.numberOfRecentEmojis() > 0 ? 0 : 1;
        emojisPager.setCurrentItem(startIndex);
        onPageSelected(startIndex);

        StickerSettings stickerSettings = builder.stickerSettings;

        if (stickerSettings != null) {

            setupStickersTab(context, emojisPager, emojisTab, stickerSettings);
        }

    }

    private void setupStickersTab(Context context, final ViewPager emojisPager, final LinearLayout emojisTab, StickerSettings stickerSettings) {
        final ImageButton emojiTab = findViewById(R.id.img_btn_emojis);

        final ImageButton stickerTab = findViewById(R.id.img_btn_stickers);

        emojiTab.setColorFilter(themeAccentColor, PorterDuff.Mode.SRC_IN);

        stickerTab.setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);


        emojiTab.setImageResource(stickerSettings.getEmojiTabIcon());
        stickerTab.setImageResource(stickerSettings.getStickerTabIcon());

        emojiTab.setVisibility(VISIBLE);
        stickerTab.setVisibility(VISIBLE);

        final RecyclerView recyclerView = findViewById(R.id.recycler_stickers);

        recyclerView.setLayoutManager(new GridAutofitLayoutManager(context, Utils.dpToPx(context, 100/*100dp same as xml*/), RecyclerView.VERTICAL, false));
        StickerAdapter adapter = new StickerAdapter(stickerSettings.getStickers());
        adapter.setStickerListener(stickerSettings.getStickerListener());
        recyclerView.setAdapter(adapter);

        stickerTab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                emojisPager.setVisibility(GONE);
                emojisTab.setVisibility(GONE);
                recyclerView.setVisibility(VISIBLE);
                emojiTab.setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
                stickerTab.setColorFilter(themeAccentColor, PorterDuff.Mode.SRC_IN);
            }
        });


        emojiTab.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                emojisPager.setVisibility(VISIBLE);
                emojisTab.setVisibility(VISIBLE);
                recyclerView.setVisibility(GONE);
                stickerTab.setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
                emojiTab.setColorFilter(themeAccentColor, PorterDuff.Mode.SRC_IN);
            }
        });
    }


    private void handleOnClicks(final ViewPager emojisPager) {
        for (int i = 0; i < emojiTabs.length - 1; i++) {
            emojiTabs[i].setOnClickListener(new EmojiTabsClickListener(emojisPager, i));
        }

        emojiTabs[emojiTabs.length - 1].setOnTouchListener(new RepeatListener(INITIAL_INTERVAL, NORMAL_INTERVAL, new OnClickListener() {
            @Override
            public void onClick(final View view) {
                if (onEmojiBackspaceClickListener != null) {
                    onEmojiBackspaceClickListener.onEmojiBackspaceClick(view);
                }
            }
        }));
    }

    public void setOnEmojiBackspaceClickListener(@Nullable final OnEmojiBackspaceClickListener onEmojiBackspaceClickListener) {
        this.onEmojiBackspaceClickListener = onEmojiBackspaceClickListener;
    }

    private ImageButton inflateButton(final Context context, @DrawableRes final int icon, @StringRes final int categoryName, final ViewGroup parent) {
        final ImageButton button = (ImageButton) LayoutInflater.from(context).inflate(R.layout.emoji_view_category, parent, false);

        button.setImageDrawable(AppCompatResources.getDrawable(context, icon));
        button.setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
        button.setContentDescription(context.getString(categoryName));

        parent.addView(button);

        return button;
    }

    @Override
    public void onPageSelected(final int i) {
        if (emojiTabLastSelectedIndex != i) {
            if (i == 0) {
                emojiPagerAdapter.invalidateRecentEmojis();
            }

            if (emojiTabLastSelectedIndex >= 0 && emojiTabLastSelectedIndex < emojiTabs.length) {
                emojiTabs[emojiTabLastSelectedIndex].setSelected(false);
                emojiTabs[emojiTabLastSelectedIndex].setColorFilter(themeIconColor, PorterDuff.Mode.SRC_IN);
            }

            emojiTabs[i].setSelected(true);
            emojiTabs[i].setColorFilter(themeAccentColor, PorterDuff.Mode.SRC_IN);

            emojiTabLastSelectedIndex = i;
        }
    }

    @Override
    public void onPageScrolled(final int i, final float v, final int i2) {
        // No-op.
    }

    @Override
    public void onPageScrollStateChanged(final int i) {
        // No-op.
    }

    static class EmojiTabsClickListener implements OnClickListener {
        private final ViewPager emojisPager;
        private final int position;

        EmojiTabsClickListener(final ViewPager emojisPager, final int position) {
            this.emojisPager = emojisPager;
            this.position = position;
        }

        @Override
        public void onClick(final View v) {
            emojisPager.setCurrentItem(position);
        }
    }
}
