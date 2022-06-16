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

public class AboutAdapter extends ListAdapter<State, AboutAdapter.AboutViewHolder> {
    private View.OnClickListener onClickListener;
    private ConstraintLayout layout;

    public AboutAdapter(@NonNull DiffUtil.ItemCallback<State> diffCallback, View.OnClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public AboutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        AboutViewHolder viewHolder = null;

        switch (viewType) {
            case 1: viewHolder = new AboutAdapter.AboutViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false));
                break;
            case 2: viewHolder = new AboutAdapter.AboutViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false));
                break;
            case 3: viewHolder = new AboutAdapter.AboutViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.text, parent, false));
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
    public int getItemViewType(int position) {
        switch (getItem(position)) {
            case Error: return 1;
            case Loading: return 2;
            case Empty: return 3;
            case Loaded: return 4;
            default: return super.getItemViewType(position);
        }
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