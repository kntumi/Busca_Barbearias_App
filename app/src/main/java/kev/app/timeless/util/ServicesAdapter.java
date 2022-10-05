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
import kev.app.timeless.model.Serviço;

public class ServicesAdapter extends ListAdapter<Serviço, ServicesAdapter.ServicoViewHolder> {
    private final View.OnClickListener onClickListener;
    private final View.OnLongClickListener onLongClickListener;

    public ServicesAdapter(@NonNull DiffUtil.ItemCallback<Serviço> diffCallback, View.OnClickListener onClickListener, View.OnLongClickListener onLongClickListener) {
        super(diffCallback);
        this.onClickListener = onClickListener;
        this.onLongClickListener = onLongClickListener;
    }

    @NonNull
    @Override
    public ServicoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ServicoViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_adapter, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ServicoViewHolder holder, int position) {
        Serviço serviço = getItem(position);

        if (serviço == null) {
            return;
        }

        holder.atualizarView(holder.itemView.findViewById(R.id.txt), serviço.getNome());
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ServicoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        holder.itemView.setOnClickListener(onClickListener);
        holder.itemView.setOnLongClickListener(onLongClickListener);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ServicoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnLongClickListener(null);
    }

    public static class ServicoViewHolder extends RecyclerView.ViewHolder{
        public ServicoViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        protected void atualizarView(AppCompatTextView textView, String txt){
            textView.setText(txt);
        }
    }
}