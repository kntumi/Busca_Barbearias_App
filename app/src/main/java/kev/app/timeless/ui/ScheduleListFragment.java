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

import com.google.android.material.chip.Chip;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import kev.app.timeless.model.Horário;
import kev.app.timeless.util.ScheduleAdapter;

public class ScheduleListFragment extends Fragment {
    private RecyclerView recyclerView;
    private Observer<List<Horário>> observer;
    private LinearLayoutManager linearLayoutManager;
    private FragmentResultListener fragmentResultListener;
    private View.OnLongClickListener onLongClickListener;
    private ScheduleAdapter adapter;
    private View.OnClickListener onClickListener;
    private Disposable disposable;
    private Integer position;
    private Bundle bundle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        recyclerView = new RecyclerView(requireActivity());
        return recyclerView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        observer = horário -> adapter.submitList(horário);
        bundle = new Bundle();
        fragmentResultListener = this::observarResult;
        onClickListener = this::observarOnClick;
        onLongClickListener = TextUtils.equals(requireParentFragment().getClass().getSimpleName(), "MineScheduleFragment") ? this::observarLongClick : null;
        linearLayoutManager = new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false);
        adapter = new ScheduleAdapter(new DiffUtil.ItemCallback<Horário>() {
            @Override
            public boolean areItemsTheSame(@NonNull Horário oldItem, @NonNull Horário newItem) {
                return oldItem.equals(newItem);
            }

            @Override
            public boolean areContentsTheSame(@NonNull Horário oldItem, @NonNull Horário newItem) {
                return oldItem.getDia() == newItem.getDia() && oldItem.getHoraAbertura() == newItem.getHoraAbertura() && oldItem.getHoraEncerramento() == newItem.getHoraEncerramento();
            }
        }, onClickListener, onLongClickListener);
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
        fragmentResultListener = null;
        onLongClickListener = null;
        onClickListener = null;
        bundle = null;
        observer = null;
        linearLayoutManager = null;
        recyclerView = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        //binding.txtHorario.setText(savedInstanceState == null ? "Não foi escolhido nenhum dia da semana" : binding.txtHorario.getText());
    }

    public Observer<List<Horário>> getObserver() {
        return observer;
    }

    public void observarOnClick(View view) {
        bundle.putInt("position", linearLayoutManager.getPosition(view));
        requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().requireParentFragment().getClass().getSimpleName(), bundle);
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

        for (int i = 0; i < linearLayoutManager.getChildCount(); i++) {
            ((Chip) linearLayoutManager.getChildAt(i)).setChecked(linearLayoutManager.getPosition(linearLayoutManager.getChildAt(i)) == position);
        }

        if (linearLayoutManager.getChildCount() == 0) {
            if (disposable != null) {
                disposable.dispose();
            }

            disposable = Observable.interval(100, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread()).subscribe(this::observarChildCount);

            return;
        }

        if (position == -1) {
            //binding.txtHorario.setText("Não foi escolhido nenhum dia da semana");
            return;
        }

        Horário horário = adapter.getCurrentList().get(position);

        String horaAbertura = String.valueOf(horário.getHoraAbertura()), horaEncerramento = String.valueOf(horário.getHoraEncerramento());
        horaAbertura = horaAbertura.length() - horaAbertura.indexOf(".") == 2 ? horaAbertura.concat("0") : horaAbertura;
        horaEncerramento = horaEncerramento.length() - horaEncerramento.indexOf(".") == 2 ? horaEncerramento.concat("0") : horaEncerramento;

        //binding.txtHorario.setText("Aberto das ".concat(horaAbertura.replace(".", ":")).concat(" até ".concat(horaEncerramento).replace(".", ":")));
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
        bundle.putInt("idToRemove", adapter.getCurrentList().get(linearLayoutManager.getPosition(view)).getDia());
        requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), bundle);
        return true;
    }
}