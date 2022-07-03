package kev.app.timeless.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import kev.app.timeless.R;
import kev.app.timeless.api.Service;
import kev.app.timeless.databinding.NotAboutBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.viewmodel.MapViewModel;

public class NotAbout extends DaggerFragment {
    private NotAboutBinding binding;
    private List<ListenerRegistration> listenerRegistrations;
    private FragmentResultListener fragmentResultListener;
    private View.OnClickListener onClickListener;
    private MapViewModel viewModel;

    @Inject
    Service service;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.not_about, container, false);
        return binding.notAbout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        onClickListener = (View.OnClickListener) requireParentFragment();
        fragmentResultListener = this::observarParent;
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, fragmentResultListener);
        binding.txtContacto.setOnClickListener(onClickListener);
        binding.txtHorario.setOnClickListener(onClickListener);
        binding.txtServicos.setOnClickListener(onClickListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());

        if (listenerRegistrations != null) {
            for (ListenerRegistration listenerRegistration : listenerRegistrations) {
                listenerRegistration.remove();
            }

            listenerRegistrations.clear();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        onClickListener = null;
        viewModel = null;
        fragmentResultListener = null;
        listenerRegistrations = null;
        binding = null;
    }

    private void observarParent(String requestKey, Bundle result) {
        if (TextUtils.isEmpty(result.getString("id"))) {
            return;
        }

        if (listenerRegistrations == null) {
            listenerRegistrations = new ArrayList<>();
        } else {
            for (ListenerRegistration listenerRegistration : listenerRegistrations) {
                listenerRegistration.remove();
            }

            listenerRegistrations.clear();
        }

        listenerRegistrations.add(service.getFirestore().collection("Barbearia").document(result.getString("id")).addSnapshotListener(this::observarDocument));
        listenerRegistrations.add(service.getFirestore().collection("Barbearia").document(result.getString("id")).collection("contactos").addSnapshotListener(this::observarCollection));
    }

    private void observarCollection(QuerySnapshot querySnapshot, FirebaseFirestoreException error) {
        try {
            binding.txtContacto.setVisibility(querySnapshot.isEmpty() ? View.GONE : View.VISIBLE);

            if (binding.txtContacto.getVisibility() == View.GONE) {
                return;
            }

            String nrPorMostrar = "";
            boolean temNrPrincipal = false;

            for (DocumentSnapshot documentSnapshot : querySnapshot) {
                for (String key : documentSnapshot.getData().keySet()) {

                    String posicao = String.valueOf(documentSnapshot.getData().get(key));

                    temNrPrincipal = TextUtils.equals(posicao, "1");

                    if (temNrPrincipal) {
                        nrPorMostrar = documentSnapshot.getId();
                        break;
                    } else {

                    }
                }

                System.out.println("documentId: "+documentSnapshot.getId()+" documentValue: "+documentSnapshot.getData());
            }

            binding.txtContacto.setText("+258".concat(nrPorMostrar));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void observarDocument(DocumentSnapshot value, FirebaseFirestoreException error) {
        try {
            if (value.exists()) {
                viewModel.getEstabelecimentos().put(value.getId(), value.getData());
            } else {
                viewModel.getEstabelecimentos().remove(value.getId());
            }

            if (value.contains("nome")) {
                if (TextUtils.equals(binding.txtNome.getText(), value.getString("nome"))) {
                    return;
                }

                binding.txtNome.setText(value.getString("nome"));

            } else {
                binding.txtNome.setText("Sem nome para mostrar");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}