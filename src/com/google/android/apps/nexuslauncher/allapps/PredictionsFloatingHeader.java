package com.google.android.apps.nexuslauncher.allapps;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.FloatProperty;
import android.view.animation.Interpolator;
import com.android.launcher3.DeviceProfile;
import com.android.launcher3.Insettable;
import com.android.launcher3.Launcher;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.allapps.AllAppsContainerView.AdapterHolder;
import com.android.launcher3.allapps.FloatingHeaderView;
import com.android.launcher3.anim.Interpolators;
import com.android.launcher3.anim.PropertySetter;
import com.android.launcher3.util.ComponentKeyMapper;
import com.google.android.apps.nexuslauncher.CustomAppPredictor;
import com.google.android.apps.nexuslauncher.allapps.PredictionRowView.DividerType;
import java.util.List;

@TargetApi(26)
public class PredictionsFloatingHeader extends FloatingHeaderView implements Insettable,
        CustomAppPredictor.UiManager.Listener {
    private static final FloatProperty<PredictionsFloatingHeader> CONTENT_ALPHA = new FloatProperty<PredictionsFloatingHeader>("contentAlpha") {
        public void setValue(PredictionsFloatingHeader predictionsFloatingHeader, float f) {
            predictionsFloatingHeader.setContentAlpha(f);
        }

        public Float get(PredictionsFloatingHeader predictionsFloatingHeader) {
            return predictionsFloatingHeader.mContentAlpha;
        }
    };
    private ActionsRowView mActionsRowView;
    private float mContentAlpha;
    private final int mHeaderTopPadding;
    private boolean mIsCollapsed;
    private boolean mIsVerticalLayout;
    private PredictionRowView mPredictionRowView;
    private final CustomAppPredictor.UiManager mPredictionUiStateManager;
    private boolean mShowAllAppsLabel;

    public PredictionsFloatingHeader(Context context) {
        this(context, null);
    }

    public PredictionsFloatingHeader(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mContentAlpha = 1.0f;
        mHeaderTopPadding = context.getResources().getDimensionPixelSize(R.dimen.all_apps_header_top_padding);
        mPredictionUiStateManager = ((CustomAppPredictor) Launcher.getLauncher(context).getUserEventDispatcher()).getUiManager();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mPredictionRowView = findViewById(R.id.prediction_row);
        mActionsRowView = findViewById(R.id.actions_row);
        setShowAllAppsLabel(Utilities.ATLEAST_MARSHMALLOW);
    }

    @Override
    public void setup(AdapterHolder[] adapterHolderArr, boolean z) {
        mPredictionRowView.setup(this, mPredictionUiStateManager.isEnabled());
        mActionsRowView.setup(this);
        mTabsHidden = z;
        ActionsRowView actionsRowView = mActionsRowView;
        actionsRowView.setDisabled(mIsVerticalLayout && !mTabsHidden);
        updateExpectedHeight();
        super.setup(adapterHolderArr, z);
    }

    private void updateExpectedHeight() {
        ActionsRowView actionsRowView = mActionsRowView;
        boolean z = mShowAllAppsLabel && mTabsHidden && mActionsRowView.shouldDraw();
        actionsRowView.setShowAllAppsLabel(z);
        DividerType dividerType = DividerType.NONE;
        if (mShowAllAppsLabel && mTabsHidden && !mActionsRowView.shouldDraw()) {
            dividerType = DividerType.ALL_APPS_LABEL;
        } else if (mTabsHidden && !mActionsRowView.shouldDraw()) {
            dividerType = DividerType.LINE;
        }
        mPredictionRowView.setDividerType(dividerType);
        mMaxTranslation = mPredictionRowView.getExpectedHeight() + mActionsRowView.getExpectedHeight();
    }

    @Override
    public int getMaxTranslation() {
        if (mMaxTranslation == 0 && mTabsHidden) {
            return getResources().getDimensionPixelSize(R.dimen.all_apps_search_bar_bottom_padding);
        }
        if (mMaxTranslation <= 0 || !mTabsHidden) {
            return mMaxTranslation;
        }
        return mMaxTranslation + getPaddingTop();
    }

    public PredictionRowView getPredictionRowView() {
        return mPredictionRowView;
    }

    public ActionsRowView getActionsRowView() {
        return mActionsRowView;
    }

    @Override
    public void setInsets(Rect rect) {
        DeviceProfile deviceProfile = Launcher.getLauncher(getContext()).getDeviceProfile();
        int i = deviceProfile.desiredWorkspaceLeftRightMarginPx + deviceProfile.cellLayoutPaddingLeftRightPx;
        mPredictionRowView.setPadding(i, mPredictionRowView.getPaddingTop(), i, mPredictionRowView.getPaddingBottom());
        mIsVerticalLayout = deviceProfile.isVerticalBarLayout();
        mActionsRowView.setDisabled(mIsVerticalLayout && !mTabsHidden);
    }

    public void headerChanged() {
        int i = mMaxTranslation;
        updateExpectedHeight();
        if (mMaxTranslation != i) {
            Launcher.getLauncher(getContext()).getAppsView().setupHeader();
        }
    }

    @Override
    protected void applyScroll(int uncappedY, int currentY) {
        if (uncappedY < currentY - mHeaderTopPadding) {
            mPredictionRowView.setScrolledOut(true);
            mActionsRowView.setHidden(true);
            return;
        }
        mActionsRowView.setHidden(false);
        mActionsRowView.setTranslationY(uncappedY);
        mPredictionRowView.setScrolledOut(false);
        mPredictionRowView.setScrollTranslation(uncappedY);
    }

    @Override
    public void setContentVisibility(boolean hasHeader, boolean hasContent, PropertySetter setter) {
        setContentVisibility(hasHeader, hasContent, setter, Interpolators.LINEAR);
    }

    public void setContentVisibility(boolean hasHeader, boolean hasContent, PropertySetter propertySetter, Interpolator interpolator) {
        if (hasHeader && !hasContent && mIsCollapsed) {
            Launcher.getLauncher(getContext()).getAppsView().getSearchUiManager().resetSearch();
        }
        allowTouchForwarding(hasContent);
        propertySetter.setFloat(this, CONTENT_ALPHA, hasContent ? 1.0f : 0.0f, interpolator);
        mPredictionRowView.setContentVisibility(hasHeader, hasContent, propertySetter, interpolator);
    }

    public void setShowAllAppsLabel(boolean show) {
        if (mShowAllAppsLabel != show) {
            mShowAllAppsLabel = show;
            headerChanged();
        }
    }

    private void setContentAlpha(float alpha) {
        mContentAlpha = alpha;
        mTabLayout.setAlpha(alpha);
        mActionsRowView.setAlpha(alpha);
    }

    public boolean hasVisibleContent() {
        return mPredictionUiStateManager.isEnabled();
    }

    public void setCollapsed(boolean collapsed) {
        if (collapsed != mIsCollapsed) {
            mIsCollapsed = collapsed;
            mActionsRowView.setCollapsed(collapsed);
            mPredictionRowView.setCollapsed(collapsed);
            headerChanged();
        }
    }

    public void setPredictedApps(boolean z, List<ComponentKeyMapper> list) {
        mPredictionRowView.setPredictedApps(z, list);
    }

    @Override
    public void onPredictionsUpdated() {
        setPredictedApps(mPredictionUiStateManager.isEnabled(), mPredictionUiStateManager.getPredictions());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        mPredictionUiStateManager.addListener(this);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mPredictionUiStateManager.removeListener(this);
    }
}