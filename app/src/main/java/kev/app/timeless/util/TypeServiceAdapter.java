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

import kev.app.timeless.R;

    public class TypeServiceAdapter extends ListAdapter<State, TypeServiceAdapter.ViewHolder> {
    private final View.OnClickListener onClickListener;

    public TypeServiceAdapter(@NonNull DiffUtil.ItemCallback<State> diffCallback, View.OnClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ViewHolder viewHolder = null;

        switch (viewType) {
            case 1: viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false));
                break;
            case 2: viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false));
                break;
            case 3: viewHolder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.text, parent, false));
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        State state = getItem(position);

        if (state != State.Empty) {
            return;
        }

        AppCompatTextView appCompatTextView = holder.itemView.findViewById(R.id.txt);
        appCompatTextView.setPrecomputedText(PrecomputedTextCompat.create("Adicione o seu primeiro contacto para os outros o verem", appCompatTextView.getTextMetricsParamsCompat()));
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
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnClickListener(onClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.setOnClickListener(null);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
