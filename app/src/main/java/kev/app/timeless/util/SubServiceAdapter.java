package kev.app.timeless.util;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.text.PrecomputedTextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import kev.app.timeless.R;

public class SubServiceAdapter extends ListAdapter<State, SubServiceAdapter.SubServiceViewHolder> {
    private final View.OnClickListener onClickListener;

    public SubServiceAdapter(@NonNull DiffUtil.ItemCallback<State> diffCallback, View.OnClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public SubServiceAdapter.SubServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        SubServiceAdapter.SubServiceViewHolder viewHolder = null;

        switch (viewType) {
            case 1: viewHolder = new SubServiceAdapter.SubServiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false));
                break;
            case 2: viewHolder = new SubServiceAdapter.SubServiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false));
                break;
            case 3: viewHolder = new SubServiceAdapter.SubServiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.text, parent, false));
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull SubServiceAdapter.SubServiceViewHolder holder, int position) {
        State state = getItem(position);

        if (state != State.Empty) {
            return;
        }

        AppCompatTextView appCompatTextView = holder.itemView.findViewById(R.id.txt);

        if (!TextUtils.isEmpty(appCompatTextView.getText())) {
            return;
        }

        appCompatTextView.setPrecomputedText(PrecomputedTextCompat.create("Adicione o seu primeiro serviço para os outros usuários o verem", appCompatTextView.getTextMetricsParamsCompat()));
    }

    @Override
    public int getItemViewType(int position) {
        switch (getItem(position)) {
            case Error: return 1;
            case Loading: return 2;
            case Empty: return 3;
            default: return super.getItemViewType(position);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull SubServiceAdapter.SubServiceViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (holder.getItemViewType() != 1) {
            return;
        }

        MaterialButton button = holder.itemView.findViewById(R.id.retryBtn);
        button.setOnClickListener(onClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull SubServiceAdapter.SubServiceViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if (holder.getItemViewType() != 1) {
            return;
        }

        MaterialButton button = holder.itemView.findViewById(R.id.retryBtn);
        button.setOnClickListener(null);
    }

    public static class SubServiceViewHolder extends RecyclerView.ViewHolder {
        public SubServiceViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
