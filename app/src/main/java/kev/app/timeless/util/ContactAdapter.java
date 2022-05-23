package kev.app.timeless.util;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import kev.app.timeless.R;
import kev.app.timeless.ui.ContactsFragment;

public class ContactAdapter extends ListAdapter<ContactsFragment.State, ContactAdapter.ContactViewHolder> {
    private View.OnClickListener onClickListener;
    private State<ContactsFragment.State> state;

    public ContactAdapter(@NonNull DiffUtil.ItemCallback<ContactsFragment.State> diffCallback, View.OnClickListener onClickListener, State<ContactsFragment.State> state) {
        super(diffCallback);
        this.onClickListener = onClickListener;
        this.state = state;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ContactViewHolder viewHolder = null;

        switch (state.value()) {
            case Error: viewHolder = new ContactViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.error, parent, false));
                break;
            case Loading: viewHolder = new ContactViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.loading, parent, false));
                break;
        }

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {

    }

    @Override
    public void onViewAttachedToWindow(@NonNull ContactViewHolder holder) {
        super.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ContactViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}