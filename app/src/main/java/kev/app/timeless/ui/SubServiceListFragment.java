package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import kev.app.timeless.model.SubServiço;
import kev.app.timeless.util.SubServiceAdapter;

public class SubServiceListFragment extends Fragment {
    private RecyclerView recyclerView;
    private SubServiceAdapter adapter;
    private Observer<List<SubServiço>> observer;
    private ItemTouchHelper itemTouchHelper;
    private LinearLayoutManager linearLayoutManager;
    private Bundle bundle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerView = new RecyclerView(requireContext());
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        itemTouchHelper = TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MySubServiceFragment") ? new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                if (bundle == null) {
                    bundle = new Bundle();
                } else {
                    bundle.clear();
                }

                bundle.putString(direction == ItemTouchHelper.RIGHT ? "idToUpdate" : "idToRemove", adapter.getCurrentList().get(linearLayoutManager.getPosition(viewHolder.itemView)).getId());
                requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), bundle);
            }
        }) : null;
        observer = subServiços -> adapter.submitList(subServiços);
        adapter = new SubServiceAdapter(new DiffUtil.ItemCallback<SubServiço>() {
            @Override
            public boolean areItemsTheSame(@NonNull SubServiço oldItem, @NonNull SubServiço newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull SubServiço oldItem, @NonNull SubServiço newItem) {
                return oldItem.getPreço().equals(newItem.getPreço());
            }
        });
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(recyclerView);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        recyclerView.setLayoutManager(null);
        bundle = null;
        adapter = null;
        itemTouchHelper = null;
        linearLayoutManager = null;
        recyclerView = null;
    }

    public Observer<List<SubServiço>> getObserver() {
        return observer;
    }
}