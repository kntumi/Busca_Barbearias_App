package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import kev.app.timeless.R;
import kev.app.timeless.model.TipoServiço;

public class TypeServiceAdapter extends ListAdapter<TipoServiço, TypeServiceAdapter.TypeServiceViewHolder> {
    private View.OnClickListener onClickListener;
    private View.OnLongClickListener onLongClickListener;

    public TypeServiceAdapter(@NonNull DiffUtil.ItemCallback<TipoServiço> diffCallback, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public TypeServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new TypeServiceAdapter.TypeServiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull TypeServiceViewHolder holder, int position) {
        holder.atualizarView(holder.itemView.findViewById(R.id.txt), getItem(position).getNome());
    }

    @Override
    public void onViewAttachedToWindow(@NonNull TypeServiceViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnClickListener(onClickListener);
        holder.itemView.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull TypeServiceViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
    }

    public static class TypeServiceViewHolder extends RecyclerView.ViewHolder{
        public TypeServiceViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        protected void atualizarView(AppCompatTextView textView, String txt){
            textView.setText(txt);
        }
    }
}
