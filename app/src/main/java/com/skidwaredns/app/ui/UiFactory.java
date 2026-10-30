package com.skidwaredns.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * AMOLED Matte + Sky-Blue (#5CC8FF) Design System inspired by RethinkDNS, NymVPN,
 * InviZible, and Tunnel Forge.
 */
public final class UiFactory {

    public static final int COLOR_BG = Color.parseColor("#07080B");
    public static final int COLOR_SURFACE = Color.parseColor("#101218");
    public static final int COLOR_CARD = Color.parseColor("#161920");
    public static final int COLOR_CARD_TOP = Color.parseColor("#1C2028");
    public static final int COLOR_CARD_SELECTED = Color.parseColor("#1B2838");
    public static final int COLOR_ICON_BG = Color.parseColor("#22262F");
    public static final int COLOR_INPUT_BG = Color.parseColor("#0E1015");
    public static final int COLOR_BORDER = Color.parseColor("#2C313C");
    public static final int COLOR_DIVIDER = Color.parseColor("#262B34");

    public static final int COLOR_SKY = Color.parseColor("#6AD2FF");
    public static final int COLOR_CYAN = Color.parseColor("#6AD2FF");
    public static final int COLOR_EMERALD = Color.parseColor("#34D399");
    public static final int COLOR_AMBER = Color.parseColor("#FBBF24");
    public static final int COLOR_CRIMSON = Color.parseColor("#EC4E6E");
    public static final int COLOR_PURPLE = Color.parseColor("#A78BFA");
    public static final int COLOR_TEXT = Color.parseColor("#F4F7FB");
    public static final int COLOR_MUTED = Color.parseColor("#8B95A7");
    public static final int COLOR_SWITCH_THUMB_ON = Color.parseColor("#F8FBFF");
    public static final int COLOR_SWITCH_THUMB_OFF = Color.parseColor("#C5CDD8");
    public static final int COLOR_SWITCH_TRACK_ON = Color.parseColor("#5CC8FF");
    public static final int COLOR_SWITCH_TRACK_OFF = Color.parseColor("#3A414D");

    public interface ChipSelectListener {
        void onSelected(String value);
    }

    public interface ToggleListener {
        void onToggled(boolean checked);
    }

    public interface SliderValueFormatter {
        String format(int stepIndex);
    }

    public interface SliderChangeListener {
        void onStepSelected(int stepIndex);
    }

    public static final class RadioItem {
        public final String value;
        public final String title;
        public final String description;

        public RadioItem(String value, String title, String description) {
            this.value = value;
            this.title = title;
            this.description = description;
        }
    }

    private UiFactory() {}

    /** Sky-blue / graphite Switch tint — replaces the default green/magenta DeviceDefault colors. */
    public static void tintSwitch(Switch sw) {
        if (sw == null) return;
        ColorStateList thumb = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{COLOR_SWITCH_THUMB_ON, COLOR_SWITCH_THUMB_OFF}
        );
        ColorStateList track = new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{-android.R.attr.state_checked}
                },
                new int[]{COLOR_SWITCH_TRACK_ON, COLOR_SWITCH_TRACK_OFF}
        );
        if (Build.VERSION.SDK_INT >= 23) {
            sw.setThumbTintList(thumb);
            sw.setTrackTintList(track);
            sw.setThumbTintMode(PorterDuff.Mode.SRC_IN);
            sw.setTrackTintMode(PorterDuff.Mode.SRC_IN);
        }
        if (Build.VERSION.SDK_INT >= 31) {
            try {
                sw.setThumbTintList(thumb);
                sw.setTrackTintList(track);
            } catch (Throwable ignored) {}
        }
    }

    public static ImageView iconView(Context ctx, int kind, int color, float dpSize) {
        ImageView iv = SkidIcons.view(ctx, kind, color, dpSize);
        int px = dp(ctx, dpSize);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px, px);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        return iv;
    }

    /** Accent-tinted rounded well with a centered line icon. */
    public static ImageView iconWell(Context ctx, int kind, int accent, float wellDp, float iconDp) {
        ImageView iv = new ImageView(ctx);
        int well = dp(ctx, wellDp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(well, well);
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER);
        iv.setImageBitmap(SkidIcons.bitmap(kind, accent, dp(ctx, iconDp)));
        int bg = mixColor(COLOR_ICON_BG, accent, 0.22f);
        iv.setBackground(roundedRect(ctx, bg, mixColor(accent, COLOR_BORDER, 0.55f), 0.7f, wellDp * 0.30f));
        return iv;
    }

    public static int mixColor(int base, int tint, float amt) {
        if (amt < 0f) amt = 0f;
        if (amt > 1f) amt = 1f;
        int br = Color.red(base), bg = Color.green(base), bb = Color.blue(base);
        int tr = Color.red(tint), tg = Color.green(tint), tb = Color.blue(tint);
        return Color.rgb(
                (int) (br + (tr - br) * amt),
                (int) (bg + (tg - bg) * amt),
                (int) (bb + (tb - bb) * amt));
    }

    public static int dp(Context ctx, float dp) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, ctx.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable roundedRect(Context ctx, int fillColor, int strokeColor, float strokeDp, float radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.RECTANGLE);
        gd.setColor(fillColor);
        gd.setCornerRadius(dp(ctx, radiusDp));
        if (strokeDp > 0f) {
            gd.setStroke(Math.max(1, dp(ctx, strokeDp)), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable pill(Context ctx, int fillColor, int strokeColor, float strokeDp) {
        return roundedRect(ctx, fillColor, strokeColor, strokeDp, 28f);
    }

    public static GradientDrawable circleRing(Context ctx, int fillColor, int strokeColor, float strokeDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(fillColor);
        if (strokeDp > 0f) {
            gd.setStroke(Math.max(1, dp(ctx, strokeDp)), strokeColor);
        }
        return gd;
    }

    public static GradientDrawable cardFill(Context ctx, int borderColor, float radiusDp) {
        GradientDrawable gd = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{COLOR_CARD_TOP, COLOR_CARD});
        gd.setCornerRadius(dp(ctx, radiusDp));
        if (borderColor != Color.TRANSPARENT) {
            gd.setStroke(Math.max(1, dp(ctx, 1f)), borderColor);
        }
        return gd;
    }

    public static LinearLayout card(Context ctx, int borderColor) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 16);
        box.setPadding(pad, pad, pad, pad);
        box.setBackground(cardFill(ctx, borderColor == Color.TRANSPARENT ? Color.TRANSPARENT : COLOR_BORDER, 18f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 14);
        box.setLayoutParams(lp);
        return box;
    }

    public static View divider(Context ctx) {
        View v = new View(ctx);
        v.setBackgroundColor(COLOR_DIVIDER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(ctx, 1));
        lp.topMargin = dp(ctx, 10);
        lp.bottomMargin = dp(ctx, 10);
        lp.leftMargin = dp(ctx, 36);
        v.setLayoutParams(lp);
        return v;
    }

    public static TextView categoryTitle(Context ctx, String title) {
        TextView tv = new TextView(ctx);
        tv.setText(title);
        tv.setTextColor(COLOR_SKY);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tv.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        tv.setLetterSpacing(0.08f);
        tv.setAllCaps(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 8);
        lp.topMargin = dp(ctx, 6);
        lp.bottomMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static TextView sectionHeader(Context ctx, String title, String subtitle) {
        TextView tv = new TextView(ctx);
        tv.setText(subtitle == null || subtitle.isEmpty() ? title : title + "  •  " + subtitle);
        tv.setTextColor(COLOR_SKY);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        tv.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 2);
        lp.bottomMargin = dp(ctx, 8);
        tv.setLayoutParams(lp);
        return tv;
    }

    public static TextView label(Context ctx, String text, float sp, int color, boolean bold, boolean mono) {
        TextView tv = new TextView(ctx);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        tv.setTypeface(mono ? Typeface.MONOSPACE : Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL);
        return tv;
    }

    public static View backHeader(Context ctx, String screenTitle, String subtitle, View.OnClickListener onBack) {
        LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 12);
        bar.setLayoutParams(lp);

        ImageView backBtn = iconWell(ctx, SkidIcons.CHEVRON_LEFT, COLOR_TEXT, 40f, 18f);
        LinearLayout.LayoutParams bLp = (LinearLayout.LayoutParams) backBtn.getLayoutParams();
        bLp.rightMargin = dp(ctx, 12);
        backBtn.setLayoutParams(bLp);
        backBtn.setOnClickListener(onBack);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(label(ctx, screenTitle, 18f, COLOR_TEXT, true, false));
        if (subtitle != null && !subtitle.isEmpty()) {
            col.addView(label(ctx, subtitle, 12f, COLOR_MUTED, false, false));
        }

        bar.addView(backBtn);
        bar.addView(col);
        return bar;
    }

    /**
     * Configure Hub Card row (matches Screenshot_20261028_162944_Rethink.jpg).
     */
    public static View configureHubCard(Context ctx, String glyph, String title, String subtitle, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int padH = dp(ctx, 14);
        int padV = dp(ctx, 15);
        row.setPadding(padH, padV, padH, padV);
        row.setBackground(cardFill(ctx, COLOR_BORDER, 18f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(ctx, 10);
        row.setLayoutParams(lp);
        row.setOnClickListener(onClick);

        ImageView iconBox = iconWell(ctx, SkidIcons.iconForGlyph(glyph), COLOR_SKY, 46f, 22f);
        LinearLayout.LayoutParams iLp = (LinearLayout.LayoutParams) iconBox.getLayoutParams();
        iLp.rightMargin = dp(ctx, 14);
        iconBox.setLayoutParams(iLp);

        LinearLayout col = new LinearLayout(ctx);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        col.addView(label(ctx, title, 16f, COLOR_TEXT, true, false));
        col.addView(label(ctx, subtitle, 12.5f, COLOR_MUTED, false, false));

        ImageView chev = iconView(ctx, SkidIcons.CHEVRON_RIGHT, COLOR_MUTED, 16f);

        row.addView(iconBox);
        row.addView(col);
        row.addView(chev);
        return row;
    }

    /**
     * Settings row with leading glyph, title, multi-line description, and Switch.
     */
    public static View iconSwitchRow(Context ctx, String glyph, String title, String desc,
                                     boolean checked, final ToggleListener listener) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 2), dp(ctx, 6), dp(ctx, 2), dp(ctx, 6));

        ImageView icon = iconWell(ctx, SkidIcons.iconForGlyph(glyph), COLOR_SKY, 32f, 16f);
        LinearLayout.LayoutParams iLp = (LinearLayout.LayoutParams) icon.getLayoutParams();
        iLp.rightMargin = dp(ctx, 12);
        icon.setLayoutParams(iLp);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.rightMargin = dp(ctx, 10);
        textCol.setLayoutParams(colLp);

        textCol.addView(label(ctx, title, 15f, COLOR_TEXT, true, false));
        if (desc != null && !desc.isEmpty()) {
            TextView d = label(ctx, desc, 12.5f, COLOR_MUTED, false, false);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dLp.topMargin = dp(ctx, 2);
            d.setLayoutParams(dLp);
            textCol.addView(d);
        }

        final Switch sw = new Switch(ctx);
        sw.setChecked(checked);
        tintSwitch(sw);
        sw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onToggled(sw.isChecked());
                }
            }
        });

        row.addView(icon);
        row.addView(textCol);
        row.addView(sw);
        return row;
    }

    public static View buildSwitchRow(Context ctx, String title, String desc, boolean checked, ToggleListener listener) {
        return iconSwitchRow(ctx, "◈", title, desc, checked, listener);
    }

    /**
     * Settings row with leading glyph, title, current value/subtitle, and trailing › chevron.
     */
    public static View iconNavRow(Context ctx, String glyph, String title, String valueSubtitle,
                                  int subtitleColor, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 2), dp(ctx, 8), dp(ctx, 2), dp(ctx, 8));
        row.setOnClickListener(onClick);

        ImageView icon = iconWell(ctx, SkidIcons.iconForGlyph(glyph), COLOR_SKY, 32f, 16f);
        LinearLayout.LayoutParams iLp = (LinearLayout.LayoutParams) icon.getLayoutParams();
        iLp.rightMargin = dp(ctx, 12);
        icon.setLayoutParams(iLp);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        textCol.addView(label(ctx, title, 15f, COLOR_TEXT, true, false));
        if (valueSubtitle != null && !valueSubtitle.isEmpty()) {
            TextView sub = label(ctx, valueSubtitle, 12.5f, subtitleColor, false, false);
            LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sLp.topMargin = dp(ctx, 2);
            sub.setLayoutParams(sLp);
            textCol.addView(sub);
        }

        ImageView chev = iconView(ctx, SkidIcons.CHEVRON_RIGHT, COLOR_MUTED, 14f);

        row.addView(icon);
        row.addView(textCol);
        row.addView(chev);
        return row;
    }

    /**
     * Interactive SeekBar Slider Row (matches Idle timeout & Socket Buffer Size in TCP/IP screenshots).
     */
    public static View iconSliderRow(Context ctx, String glyph, String title, String desc,
                                     int stepCount, int initialStep,
                                     final SliderValueFormatter formatter,
                                     final SliderChangeListener listener) {
        LinearLayout outer = new LinearLayout(ctx);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(ctx, 2), dp(ctx, 6), dp(ctx, 2), dp(ctx, 6));

        LinearLayout topRow = new LinearLayout(ctx);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = iconView(ctx, SkidIcons.iconForGlyph(glyph), COLOR_SKY, 18f);
        LinearLayout.LayoutParams iLp = new LinearLayout.LayoutParams(dp(ctx, 28), dp(ctx, 28));
        iLp.rightMargin = dp(ctx, 10);
        icon.setLayoutParams(iLp);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        colLp.rightMargin = dp(ctx, 8);
        textCol.setLayoutParams(colLp);
        textCol.addView(label(ctx, title, 15f, COLOR_TEXT, true, false));
        textCol.addView(label(ctx, desc, 12.5f, COLOR_MUTED, false, false));

        int clampedInit = Math.max(0, Math.min(stepCount - 1, initialStep));
        final TextView valueLabel = label(ctx, formatter.format(clampedInit), 13.5f, COLOR_TEXT, false, false);

        topRow.addView(icon);
        topRow.addView(textCol);
        topRow.addView(valueLabel);

        SeekBar sb = new SeekBar(ctx);
        sb.setMax(Math.max(1, stepCount - 1));
        sb.setProgress(clampedInit);
        LinearLayout.LayoutParams sbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sbLp.leftMargin = dp(ctx, 30);
        sbLp.topMargin = dp(ctx, 6);
        sb.setLayoutParams(sbLp);

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                valueLabel.setText(formatter.format(progress));
                if (fromUser && listener != null) {
                    listener.onStepSelected(progress);
                }
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        outer.addView(topRow);
        outer.addView(sb);
        return outer;
    }

    /**
     * Radio Option Group Card (matches STRATEGY, TECHNIQUE, and Global DNS screenshots).
     */
    public static LinearLayout buildRadioCard(final Context ctx, final RadioItem[] items,
                                              String activeValue, final ChipSelectListener listener) {
        final LinearLayout card = card(ctx, COLOR_BORDER);
        final TextView[] dots = new TextView[items.length];

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            final RadioItem item = items[i];
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(ctx, 4), dp(ctx, 10), dp(ctx, 4), dp(ctx, 10));

            boolean isSel = item.value.equalsIgnoreCase(activeValue != null ? activeValue : "");
            final TextView radioDot = label(ctx, isSel ? "◉" : "◯", 20f, isSel ? COLOR_SKY : COLOR_MUTED, true, false);
            LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(dp(ctx, 34), ViewGroup.LayoutParams.WRAP_CONTENT);
            rLp.rightMargin = dp(ctx, 10);
            radioDot.setLayoutParams(rLp);
            dots[i] = radioDot;

            LinearLayout col = new LinearLayout(ctx);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            col.addView(label(ctx, item.title, 15.5f, COLOR_TEXT, true, false));
            if (item.description != null && !item.description.isEmpty()) {
                col.addView(label(ctx, item.description, 12.5f, COLOR_MUTED, false, false));
            }

            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int k = 0; k < dots.length; k++) {
                        boolean on = (k == index);
                        dots[k].setText(on ? "◉" : "◯");
                        dots[k].setTextColor(on ? COLOR_SKY : COLOR_MUTED);
                    }
                    if (listener != null) {
                        listener.onSelected(item.value);
                    }
                }
            });

            row.addView(radioDot);
            row.addView(col);
            card.addView(row);
        }
        return card;
    }

    public static Button actionButton(Context ctx, String text, int bgColor, int textColor, int strokeColor) {
        return actionButton(ctx, SkidIcons.iconForButtonLabel(text), text, bgColor, textColor, strokeColor);
    }

    public static Button actionButton(Context ctx, int iconKind, String text, int bgColor, int textColor, int strokeColor) {
        Button btn = new Button(ctx);
        btn.setText(text);
        btn.setAllCaps(false);
        btn.setTextColor(textColor);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        btn.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));
        btn.setBackground(roundedRect(ctx, bgColor, strokeColor, 1.1f, 14f));
        btn.setMinHeight(dp(ctx, 44));
        btn.setCompoundDrawablesWithIntrinsicBounds(SkidIcons.drawable(ctx, iconKind, textColor, 15f), null, null, null);
        btn.setCompoundDrawablePadding(dp(ctx, 6));
        return btn;
    }

    public static EditText monoInput(Context ctx, String initialValue, String hint, boolean multiLine) {
        EditText et = new EditText(ctx);
        et.setText(initialValue != null ? initialValue : "");
        et.setHint(hint);
        et.setHintTextColor(Color.parseColor("#54647B"));
        et.setTextColor(COLOR_TEXT);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
        et.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        int padH = dp(ctx, 12);
        int padV = dp(ctx, 10);
        et.setPadding(padH, padV, padH, padV);
        et.setBackground(roundedRect(ctx, COLOR_INPUT_BG, COLOR_BORDER, 1f, 10f));
        if (multiLine) {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            et.setGravity(Gravity.TOP | Gravity.START);
            et.setMinLines(3);
        } else {
            et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
            et.setSingleLine(true);
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 4);
        lp.bottomMargin = dp(ctx, 8);
        et.setLayoutParams(lp);
        return et;
    }

    public static HorizontalScrollView buildChipSelector(
            final Context ctx, final String[] options, String currentSelection,
            final int activeAccent, final ChipSelectListener listener) {
        HorizontalScrollView hsv = new HorizontalScrollView(ctx);
        hsv.setHorizontalScrollBarEnabled(false);

        final LinearLayout strip = new LinearLayout(ctx);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setPadding(0, dp(ctx, 4), 0, dp(ctx, 6));

        for (int i = 0; i < options.length; i++) {
            final String opt = options[i];
            final TextView chip = new TextView(ctx);
            chip.setText(opt);
            chip.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);
            chip.setPadding(dp(ctx, 14), dp(ctx, 8), dp(ctx, 14), dp(ctx, 8));
            boolean isSelected = opt.equalsIgnoreCase(currentSelection != null ? currentSelection : "");
            styleChip(ctx, chip, isSelected, activeAccent);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dp(ctx, 8);
            chip.setLayoutParams(lp);

            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int j = 0; j < strip.getChildCount(); j++) {
                        View child = strip.getChildAt(j);
                        if (child instanceof TextView) {
                            TextView tv = (TextView) child;
                            styleChip(ctx, tv, tv == chip, activeAccent);
                        }
                    }
                    if (listener != null) {
                        listener.onSelected(opt);
                    }
                }
            });
            strip.addView(chip);
        }

        hsv.addView(strip);
        return hsv;
    }

    // =========================================================================
    // v5.2 ORGANIZATION COMPONENTS: Collapsible Sections, Search, Task Tiles,
    // Setting Rows, Info Footnotes
    // =========================================================================

    /** Immutable descriptor of one navigable setting/feature leaf. */
    public static final class SettingEntry {
        public final String glyph;
        public final String title;
        public final String subtitle;
        public final int colorAccent;
        public final int badgeCount;   // 0 = no badge
        public final View.OnClickListener onClick;

        public SettingEntry(String glyph, String title, String subtitle, int colorAccent,
                            int badgeCount, View.OnClickListener onClick) {
            this.glyph = glyph;
            this.title = title;
            this.subtitle = subtitle;
            this.colorAccent = colorAccent;
            this.badgeCount = badgeCount;
            this.onClick = onClick;
        }
    }

    private static boolean matchesQuery(SettingEntry e, String query) {
        if (query == null || query.trim().isEmpty()) return true;
        String q = query.trim().toLowerCase();
        return (e.title != null && e.title.toLowerCase().contains(q))
                || (e.subtitle != null && e.subtitle.toLowerCase().contains(q));
    }

    /** Filters entries by title/subtitle; returns null when the query matches nothing. */
    public static List<SettingEntry> filterEntries(List<SettingEntry> all, String query) {
        List<SettingEntry> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            if (matchesQuery(all.get(i), query)) out.add(all.get(i));
        }
        return out;
    }

    /**
     * Search field for a settings hub. Fires listener after each keystroke.
     */
    public static EditText searchField(final Context ctx, String hint, final ChipSelectListener listener) {
        final EditText et = monoInput(ctx, "", hint, false);
        et.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
        et.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
        et.setSingleLine(true);
        et.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (listener != null) listener.onSelected(s.toString());
            }
        });
        return et;
    }

    /** Small right-aligned counter chip, e.g. "12 items" or "3 on". */
    public static TextView counterChip(Context ctx, String text, int accent) {
        TextView tv = label(ctx, text, 10.5f, accent, true, true);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(ctx, 8), dp(ctx, 2), dp(ctx, 8), dp(ctx, 2));
        tv.setBackground(roundedRect(ctx, COLOR_SURFACE, COLOR_BORDER, 0.8f, 8f));
        return tv;
    }

    /**
     * Collapsible section: accent title bar with glyph + count chip + expand chevron,
     * then a card body containing the rows. Body visibility toggles in place.
     */
    public static LinearLayout collapsibleSection(final Context ctx, String glyph, String title,
                                                 String summary, int accent,
                                                 boolean expanded, final View body) {
        LinearLayout wrap = new LinearLayout(ctx);
        wrap.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams wLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        wLp.bottomMargin = dp(ctx, 10);
        wrap.setLayoutParams(wLp);

        final LinearLayout bar = new LinearLayout(ctx);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(ctx, 12), dp(ctx, 12), dp(ctx, 12), dp(ctx, 12));
        bar.setBackground(cardFill(ctx, mixColor(accent, COLOR_BORDER, 0.55f), 16f));

        ImageView ic = iconWell(ctx, SkidIcons.iconForGlyph(glyph), accent, 32f, 16f);
        LinearLayout.LayoutParams icLp = (LinearLayout.LayoutParams) ic.getLayoutParams();
        icLp.rightMargin = dp(ctx, 10);
        ic.setLayoutParams(icLp);

        LinearLayout textCol = new LinearLayout(ctx);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        textCol.addView(label(ctx, title.toUpperCase(), 12.5f, accent, true, false));
        if (summary != null && !summary.isEmpty()) {
            textCol.addView(label(ctx, summary, 11.5f, COLOR_MUTED, false, false));
        }

        final TextView chev = label(ctx, expanded ? "⌃" : "⌄", 16f, COLOR_MUTED, true, false);
        chev.setPadding(dp(ctx, 8), 0, dp(ctx, 2), 0);

        body.setVisibility(expanded ? View.VISIBLE : View.GONE);
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bLp.topMargin = dp(ctx, 1);
        body.setLayoutParams(bLp);

        bar.addView(ic);
        bar.addView(textCol);
        bar.addView(chev);
        bar.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean show = body.getVisibility() != View.VISIBLE;
                body.setVisibility(show ? View.VISIBLE : View.GONE);
                chev.setText(show ? "⌃" : "⌄");
            }
        });

        wrap.addView(bar);
        wrap.addView(body);
        return wrap;
    }

    /**
     * Task-first tile: big glyph, tool name, one-line live status, and a single
     * primary action verb. Used by the redesigned Configure hub.
     */
    public static LinearLayout taskTile(Context ctx, String glyph, String toolName, String status,
                                        String actionVerb, int accent, View.OnClickListener onClick) {
        LinearLayout tile = new LinearLayout(ctx);
        tile.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(ctx, 14);
        tile.setPadding(pad, pad, pad, pad);
        tile.setBackground(cardFill(ctx, COLOR_BORDER, 18f));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tile.setLayoutParams(lp);
        tile.setOnClickListener(onClick);

        LinearLayout top = new LinearLayout(ctx);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ic = iconWell(ctx, SkidIcons.iconForGlyph(glyph), accent, 36f, 18f);
        LinearLayout.LayoutParams icLp = (LinearLayout.LayoutParams) ic.getLayoutParams();
        icLp.rightMargin = dp(ctx, 10);
        ic.setLayoutParams(icLp);
        top.addView(ic);
        top.addView(label(ctx, toolName, 12.5f, COLOR_TEXT, true, false));

        TextView stat = label(ctx, status, 15f, accent, true, false);
        LinearLayout.LayoutParams stLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        stLp.topMargin = dp(ctx, 7);
        stat.setLayoutParams(stLp);

        TextView verb = label(ctx, actionVerb + "  ›", 11.5f, COLOR_MUTED, true, false);
        LinearLayout.LayoutParams vbLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        vbLp.topMargin = dp(ctx, 4);
        verb.setLayoutParams(vbLp);

        tile.addView(top);
        tile.addView(stat);
        tile.addView(verb);
        return tile;
    }

    /** Two task tiles side by side, split by weight, with matched gutters. */
    public static LinearLayout taskTileRow(Context ctx, LinearLayout left, LinearLayout right) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = dp(ctx, 10);
        row.setLayoutParams(rLp);
        LinearLayout.LayoutParams lLp = (LinearLayout.LayoutParams) left.getLayoutParams();
        lLp.rightMargin = dp(ctx, 10);
        row.addView(left);
        row.addView(right);
        return row;
    }

    /**
     * Density-reduced one-line setting row: glyph + title + value + chevron.
     * Replaces verbose switch rows for secondary options.
     */
    public static View compactRow(Context ctx, String glyph, String title, String value,
                                  int valueColor, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(ctx, 2), dp(ctx, 9), dp(ctx, 2), dp(ctx, 9));
        row.setOnClickListener(onClick);

        ImageView icon = iconWell(ctx, SkidIcons.iconForGlyph(glyph), COLOR_SKY, 28f, 14f);
        LinearLayout.LayoutParams iLp = (LinearLayout.LayoutParams) icon.getLayoutParams();
        iLp.rightMargin = dp(ctx, 10);
        icon.setLayoutParams(iLp);

        TextView t = label(ctx, title, 14.5f, COLOR_TEXT, false, false);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView v = label(ctx, value, 12.5f, valueColor, false, false);
        v.setPadding(dp(ctx, 6), 0, dp(ctx, 6), 0);

        TextView chev = label(ctx, "›", 18f, COLOR_MUTED, false, false);
        row.addView(icon);
        row.addView(t);
        row.addView(v);
        row.addView(chev);
        return row;
    }

    /** Sub-group caption inside a card body, e.g. "ADVANCED" / "IDENTITY". */
    public static TextView groupCaption(Context ctx, String text) {
        TextView tv = label(ctx, text, 10.5f, COLOR_MUTED, true, false);
        tv.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 2);
        lp.topMargin = dp(ctx, 10);
        lp.bottomMargin = dp(ctx, 2);
        tv.setLayoutParams(lp);
        return tv;
    }

    /** Muted explanatory footnote (replaces long inline descriptions). */
    public static TextView footnote(Context ctx, String text) {
        TextView tv = label(ctx, text, 11.5f, COLOR_MUTED, false, false);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(ctx, 2);
        lp.rightMargin = dp(ctx, 2);
        lp.topMargin = dp(ctx, 8);
        lp.bottomMargin = dp(ctx, 2);
        tv.setLayoutParams(lp);
        return tv;
    }

    /** Version/status footer shown at the bottom of the hub. */
    public static View versionFooter(Context ctx, String version, String buildInfo) {
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(ctx, 14);
        lp.bottomMargin = dp(ctx, 6);
        box.setLayoutParams(lp);
        TextView v = label(ctx, "S K I D W A R E D N S   " + version, 11f, COLOR_MUTED, true, true);
        v.setGravity(Gravity.CENTER);
        TextView b = label(ctx, buildInfo, 10.5f, Color.parseColor("#5A6478"), false, true);
        b.setGravity(Gravity.CENTER);
        box.addView(v);
        box.addView(b);
        return box;
    }

    private static void styleChip(Context ctx, TextView chip, boolean selected, int accent) {
        if (selected) {
            chip.setTextColor(COLOR_BG);
            chip.setBackground(roundedRect(ctx, accent, accent, 1f, 20f));
        } else {
            chip.setTextColor(COLOR_TEXT);
            chip.setBackground(roundedRect(ctx, COLOR_SURFACE, COLOR_BORDER, 1f, 20f));
        }
    }
}
