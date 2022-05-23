package kev.app.timeless.util;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import kev.app.timeless.R;
import kev.app.timeless.ui.AboutFragment;

public class AboutAdapter extends ListAdapter<AboutFragment.State, AboutAdapter.AboutViewHolder> {
    private State<AboutFragment.State> state;
    private View.OnClickListener onClickListener;
    private ConstraintLayout layout;
    private Info info;

    public AboutAdapter(@NonNull DiffUtil.ItemCallback<AboutFragment.State> diffCallback, State<AboutFragment.State> state, View.OnClickListener onClickListener, Info info) {
        super(diffCallback);
        this.state = state;
        this.onClickListener = onClickListener;
        this.info = info;
    }

    @NonNull
    @Override
    public AboutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AboutViewHolder viewHolder = null;

        switch (state.value()) {
            case Loaded: viewHolder = new AboutAdapter.AboutViewHolder(LayoutInflater.from(parent.getContext()).inflate(info.isUserLoggedIn() ? R.layout.about : R.layout.not_about, parent, false));
                break;
            case Error: viewHolder = new AboutAdapter.AboutViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false));
                break;
            case Loading: viewHolder = new AboutAdapter.AboutViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false));
                break;
        }

        return viewHolder;
    }

    public void atualizarNome(String nomeActual) {
        MaterialAutoCompleteTextView textView = layout.findViewById(R.id.txtNome);

        if (textView == null || TextUtils.equals(textView.getText(), nomeActual)) {
            return;
        }

        textView.setText(nomeActual);
    }

    @Override
    public void onBindViewHolder(@NonNull AboutViewHolder holder, int position) {
        layout = (ConstraintLayout) holder.itemView;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull AboutViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        ConstraintLayout layout = (ConstraintLayout) holder.itemView;

        if (layout.getChildCount() == 1) {
            View v = layout.getChildAt(0);

            if (TextUtils.equals(v.getClass().getSimpleName(), "MaterialButton")) {
                v.setOnClickListener(onClickListener);
            }

            return;
        }

        for (int i = 0 ; i < layout.getChildCount() ; i++) {
            View v = layout.getChildAt(i);

            if (v.getId() != R.id.info || v.getId() != R.id.nome) {
                v.setOnClickListener(onClickListener);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull AboutViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        ConstraintLayout layout = (ConstraintLayout) holder.itemView;

        for (int i = 0 ; i < layout.getChildCount() ; i++) {
            View v = layout.getChildAt(i);

            if (v.hasOnClickListeners()) {
                v.setOnClickListener(null);
            }
        }
    }

    public static class AboutViewHolder extends RecyclerView.ViewHolder {
        public AboutViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}