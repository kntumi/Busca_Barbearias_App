package kev.app.timeless.util;

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

public class ContactAdapter extends ListAdapter<State, ContactAdapter.ContactViewHolder> {
    private View.OnClickListener onClickListener;

    public ContactAdapter(@NonNull DiffUtil.ItemCallback<State> diffCallback, View.OnClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContactViewHolder viewHolder = null;

        switch (viewType) {
            case 1: viewHolder = new ContactViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false));
                break;
            case 2: viewHolder = new ContactViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false));
                break;
            case 3: viewHolder = new ContactViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.text, parent, false));
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        State state = getItem(position);

        if (state != State.Empty) {
            return;
        }

        AppCompatTextView appCompatTextView = holder.itemView.findViewById(R.id.txt);
        appCompatTextView.setPrecomputedText(PrecomputedTextCompat.create("Adicione o seu primeiro contacto para os outros usuários o verem", appCompatTextView.getTextMetricsParamsCompat()));
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
    public void onViewAttachedToWindow(@NonNull ContactViewHolder holder) {
        super.onViewAttachedToWindow(holder);

        if (holder.getItemViewType() != 1) {
            return;
        }

        MaterialButton button = holder.itemView.findViewById(R.id.retryBtn);
        button.setOnClickListener(onClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ContactViewHolder holder) {
        super.onViewDetachedFromWindow(holder);

        if (holder.getItemViewType() != 1) {
            return;
        }

        MaterialButton button = holder.itemView.findViewById(R.id.retryBtn);
        button.setOnClickListener(null);
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}