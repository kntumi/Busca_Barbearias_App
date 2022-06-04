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
import kev.app.timeless.model.SubServiço;

public class SubServiceListAdapter extends ListAdapter<SubServiço, SubServiceListAdapter.SubServiceViewHolder> {
    public SubServiceListAdapter(@NonNull DiffUtil.ItemCallback<SubServiço> diffCallback) {
        super(diffCallback);
    }

    @NonNull
    @Override
    public SubServiceListAdapter.SubServiceViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new SubServiceListAdapter.SubServiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sub_service, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull SubServiceListAdapter.SubServiceViewHolder holder, int position) {
        holder.atualizarViews(holder.itemView.findViewById(R.id.txtNome), holder.itemView.findViewById(R.id.txtPreco), getItem(position).getNome(), getItem(position).getPreço());
    }

    public static class SubServiceViewHolder extends RecyclerView.ViewHolder {
        public SubServiceViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        private void atualizarViews(AppCompatTextView nome, AppCompatTextView preco, Object nomeServiço, Object precoServiço) {
            preco.setText(String.valueOf(precoServiço).concat(" mts"));
            nome.setText(String.valueOf(nomeServiço));
        }
    }
}
