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

public class TypeServicesAdapter extends ListAdapter<TipoServiço, TypeServicesAdapter.ViewHolder> {
    private final View.OnClickListener onClickListener;

    public TypeServicesAdapter(@NonNull DiffUtil.ItemCallback<TipoServiço> diffCallback, View.OnClickListener onClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.atualizarView(holder.itemView.findViewById(R.id.txt), getItem(position).getNome());
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

        protected void atualizarView(AppCompatTextView textView, String txt){
            textView.setText(txt);
        }
    }
}
