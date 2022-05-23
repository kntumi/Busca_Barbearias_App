package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.model.Serviço;
import kev.app.timeless.util.ServicesAdapter;

public class ServiceListFragment extends Fragment {
    private RecyclerView recyclerView;
    private Observer<List<Serviço>> observer;
    private LinearLayoutManager linearLayoutManager;
    private FragmentResultListener fragmentResultListener;
    private View.OnLongClickListener onLongClickListener;
    private View.OnClickListener onClickListener;
    private Disposable disposable;
    private Integer position;
    private ServicesAdapter adapter;
    private Bundle bundle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerView = new RecyclerView(requireContext());
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = serviços -> adapter.submitList(serviços);
        onClickListener = this::observarOnClick;
        onLongClickListener = TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MyServicesFragment") ? this::observarLongClick : null;
        bundle = new Bundle();
        fragmentResultListener = this::observarResultado;
        adapter = new ServicesAdapter(new DiffUtil.ItemCallback<Serviço>() {
            @Override
            public boolean areItemsTheSame(@NonNull Serviço oldItem, @NonNull Serviço newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Serviço oldItem, @NonNull Serviço newItem) {
                return false;
            }
        }, onClickListener, onLongClickListener);
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

        if (disposable != null) {
            disposable.dispose();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recyclerView.setAdapter(null);
        recyclerView.setLayoutManager(null);
        bundle.clear();
        disposable = null;
        adapter = null;
        position = null;
        onClickListener = null;
        onLongClickListener = null;
        bundle = null;
        linearLayoutManager = null;
        fragmentResultListener = null;
        recyclerView = null;
    }

    public Observer<List<Serviço>> getObserver() {
        return observer;
    }

    public void observarOnClick(View view) {
        bundle.putInt("position", linearLayoutManager.getPosition(view));
        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().requireParentFragment().getClass().getSimpleName(), bundle);
    }

    private void observarResultado(String requestKey, Bundle result) {
        position = -1;
        if (result.containsKey(requireParentFragment().getClass().getSimpleName())) {
            if (result.getBundle(requireParentFragment().getClass().getSimpleName()).containsKey(getClass().getSimpleName())) {
                if (result.getBundle(requireParentFragment().getClass().getSimpleName()).getBundle(getClass().getSimpleName()).containsKey("position")) {
                    position = result.getBundle(requireParentFragment().getClass().getSimpleName()).getBundle(getClass().getSimpleName()).getInt("position");
                }
            }
        }

        for (int i = 0; i < linearLayoutManager.getChildCount(); i++) {
            if (linearLayoutManager.getPosition(linearLayoutManager.getChildAt(i)) == position) {
                System.out.println("a position eh: "+adapter.getCurrentList().get(position));
            }
        }

        if (linearLayoutManager.getChildCount() == 0) {
            if (disposable != null) {
                disposable.dispose();
            }

            System.out.println("for real dawg "+position);
            disposable = Observable.interval(50, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()).subscribe(this::observarChildCount);
            return;
        }

        Bundle b = new Bundle();
        b.putString("idServiçoEscolhido", position == -1 ? null : adapter.getCurrentList().get(position).getId());
        requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
    }

    private void observarChildCount(Long aLong) {
        if (linearLayoutManager.getChildCount() != 0) {
            Bundle b = new Bundle();

            if (position != -1) {
                b.putInt("position", position);
            }

            requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().requireParentFragment().getClass().getSimpleName(), b);
            disposable.dispose();
        }
    }

    private boolean observarLongClick(View view) {
        bundle.putString("idToRemove", adapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getId());
        requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), bundle);
        return true;
    }
}