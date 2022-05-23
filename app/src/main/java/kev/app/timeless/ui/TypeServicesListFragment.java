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
import io.reactivex.disposables.Disposable;
import kev.app.timeless.model.TipoServiço;
import kev.app.timeless.util.TypeServiceAdapter;

public class TypeServicesListFragment extends Fragment {
    private RecyclerView recyclerView;
    private TypeServiceAdapter adapter;
    private Observer<List<TipoServiço>> observer;
    private LinearLayoutManager linearLayoutManager;
    private View.OnClickListener onClickListener;
    private FragmentResultListener fragmentResultListener;
    private View.OnLongClickListener onLongClickListener;
    private Integer position;
    private Disposable disposable;
    private Bundle bundle;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerView = new RecyclerView(requireContext());
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = tipoServiços -> adapter.submitList(tipoServiços);
        bundle = new Bundle();
        onClickListener = this::observarOnClick;
        fragmentResultListener = this::observarResult;
        onLongClickListener = TextUtils.equals("MyTypeServicesFragment", requireParentFragment().getClass().getSimpleName()) ? this::observarOnLongClick : null;
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        adapter = new TypeServiceAdapter(new DiffUtil.ItemCallback<TipoServiço>() {
            @Override
            public boolean areItemsTheSame(@NonNull TipoServiço oldItem, @NonNull TipoServiço newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull TipoServiço oldItem, @NonNull TipoServiço newItem) {
                return false;
            }
        }, onClickListener, onLongClickListener);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(linearLayoutManager);
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
        linearLayoutManager = null;
        fragmentResultListener = null;
        bundle.clear();
        onLongClickListener = null;
        bundle = null;
        adapter = null;
        disposable = null;
        position = null;
        onClickListener = null;
        observer = null;
        recyclerView = null;
    }

    public Observer<List<TipoServiço>> getObserver() {
        return observer;
    }

    private void observarOnClick(View view) {
        bundle.putInt("position", linearLayoutManager.getPosition(view));
        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().requireParentFragment().getClass().getSimpleName(), bundle);
    }

    private boolean observarOnLongClick(View view) {
        bundle.putString("idToRemove", adapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getId());
        requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), bundle);
        return true;
    }

    private void observarResult(String requestKey, Bundle result) {
        position = -1;
        if (result.containsKey(requireParentFragment().getClass().getSimpleName())) {
            if (result.getBundle(requireParentFragment().getClass().getSimpleName()).containsKey(getClass().getSimpleName())) {
                if (result.getBundle(requireParentFragment().getClass().getSimpleName()).getBundle(getClass().getSimpleName()).containsKey("position")) {
                    position = result.getBundle(requireParentFragment().getClass().getSimpleName()).getBundle(getClass().getSimpleName()).getInt("position");
                }
            }
        }

        if (linearLayoutManager.getChildCount() == 0) {
            if (disposable != null) {
                disposable.dispose();
            }

            disposable = Observable.interval(50, TimeUnit.MILLISECONDS).subscribe(this::observarChildCount);
            return;
        }

        Bundle b = new Bundle();
        b.putString("idTipoServiçoEscolhido", position == -1 ? null : adapter.getCurrentList().get(position).getId());
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
}