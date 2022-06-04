package kev.app.timeless.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.android.support.DaggerFragment;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import kev.app.timeless.R;
import kev.app.timeless.databinding.LayoutDefaultBinding;
import kev.app.timeless.di.viewModelFactory.ViewModelProvidersFactory;
import kev.app.timeless.model.Contacto;
import kev.app.timeless.model.User;
import kev.app.timeless.util.ContactAdapter;
import kev.app.timeless.util.ContactsAdapter;
import kev.app.timeless.util.State;
import kev.app.timeless.viewmodel.MapViewModel;

public class ContactsFragment extends DaggerFragment implements View.OnClickListener, View.OnLongClickListener {
    private Observer<List<User>> observer;
    private LayoutDefaultBinding binding;
    private Bundle bundle, b;
    private ContactAdapter contactAdapter;
    private ContactsAdapter contactsAdapter;
    private FragmentResultListener parentResultListener;
    private ListenerRegistration listenerRegistration;
    private ItemTouchHelper itemTouchHelper;
    private ClipboardManager clipboardManager;
    private MapViewModel viewModel;
    private Disposable disposable;

    @Inject
    ViewModelProvidersFactory providerFactory;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.layout_default, container, false);
        return binding.layoutPrincipal;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity(), providerFactory).get(MapViewModel.class);
        observer = this::observarUser;
        parentResultListener = this::observarParent;
        contactAdapter = new ContactAdapter(new DiffUtil.ItemCallback<State>() {
            @Override
            public boolean areItemsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }

            @Override
            public boolean areContentsTheSame(@NonNull State oldItem, @NonNull State newItem) {
                return false;
            }
        }, this);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false));
        binding.recyclerView.setAdapter(contactAdapter);
        binding.barra.setTitle("Contactos");
    }

    @Override
    public void onResume() {
        super.onResume();
        requireParentFragment().getChildFragmentManager().setFragmentResultListener(getClass().getSimpleName(), this, parentResultListener);
        binding.barra.setNavigationOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        requireParentFragment().getChildFragmentManager().clearFragmentResultListener(getClass().getSimpleName());
        binding.barra.setNavigationOnClickListener(null);

        if (itemTouchHelper != null) {
            itemTouchHelper.attachToRecyclerView(null);
        }

        if (disposable != null) {
            if (!disposable.isDisposed()) {
                disposable.dispose();
            }

            disposable = null;
        }

        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.recyclerView.setLayoutManager(null);
        binding.recyclerView.setAdapter(null);
        binding.layoutPrincipal.removeAllViews();
        observer = null;
        b = null;
        itemTouchHelper = null;
        clipboardManager = null;
        viewModel = null;
        contactAdapter = null;
        bundle = null;
        parentResultListener = null;
        binding = null;
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        bundle = savedInstanceState == null ? new Bundle() : savedInstanceState.getBundle("bundle");
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("bundle", new Bundle(bundle));
    }

    public Observer<List<User>> getObserver() {
        return observer;
    }

    private void observarParent(String requestKey, Bundle result) {
        bundle = bundle.size() == 0 ? result : bundle;

        contactAdapter.submitList(Collections.singletonList(viewModel.getContactos().containsKey(result.getString("id")) ? State.Loaded : State.Loading), () -> {
            List<State> states = contactAdapter.getCurrentList();
            for (int i = 0 ; i < states.size(); i++) {
                switch (states.get(i)) {
                    case Loading: obterContactos(result);
                        break;
                    case Loaded: onLoad();
                        break;
                }
            }
        });
    }

    private void obterContactos(Bundle result) {
        disposable = viewModel.getService().getBarbeariaService().obterContactos(result.getString("id"))
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(maps -> {
                    viewModel.getContactos().put(result.getString("id"), maps);

                    if (maps.size() != 0) {
                        onLoad();
                        return;
                    }

                    contactAdapter.submitList(Collections.singletonList(State.Empty), this::adicionarListener);
                }, throwable -> contactAdapter.submitList(Collections.singletonList(State.Error)));
    }

    private void observarUser(List<User> users) {
        binding.barra.setOnMenuItemClickListener(null);

        if (users.size() != 0) {
            if (binding.barra.getMenu().size() == 0) {
                binding.barra.inflateMenu(R.menu.add);
            }

            binding.barra.setOnMenuItemClickListener(item -> {
                if (b == null) {
                    b = new Bundle();
                }

                b.putString("fragmentToLoad", "NewContactFragment");
                requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
                return true;
            });

            return;
        }

        for (int i = 0 ; i < binding.barra.getMenu().size() ; i++) {
            binding.barra.getMenu().removeItem(i);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case View.NO_ID: if (b == null) {
                                 b = new Bundle();
                             }

                             b.putString("fragmentToLoad", "AboutFragment");
                             requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
                break;
            case R.id.retryBtn: obterContactos(bundle);
                break;
        }
    }

    public void adicionarListener () {
        listenerRegistration = viewModel.getService().getFirestore().collection("Barbearia").document(bundle.getString("id")).collection("contactos").addSnapshotListener(this::observarCollection);
    }

    public void onLoad() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }

        if (contactsAdapter == null) {
            contactsAdapter = new ContactsAdapter(new DiffUtil.ItemCallback<Contacto>() {
                @Override
                public boolean areItemsTheSame(@NonNull Contacto oldItem, @NonNull Contacto newItem) {
                    return false;
                }

                @Override
                public boolean areContentsTheSame(@NonNull Contacto oldItem, @NonNull Contacto newItem) {
                    return false;
                }
            }, this);
        }

        adicionarListener();

        if (itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    switch (direction) {
                        case ItemTouchHelper.LEFT: if (disposable != null) {
                                                      disposable.dispose();
                                                      disposable = null;
                                                   }

                                                   String nrContacto = String.valueOf(contactsAdapter.getCurrentList().get(viewHolder.getBindingAdapterPosition()).getNrTelefone());

                                                   disposable = viewModel.getService().getBarbeariaService().removerContacto(bundle.getString("id"), nrContacto).subscribe(aBoolean -> {
                                                       if (aBoolean) {
                                                           viewModel.getContactos().get(bundle.getString("id")).remove(nrContacto);
                                                       }

                                                       Toast.makeText(requireContext(), aBoolean ? "O contacto foi removido com sucesso" : "", Toast.LENGTH_LONG).show();
                                                   }, throwable -> Toast.makeText(requireActivity(), "", Toast.LENGTH_LONG).show());
                            break;
                        case ItemTouchHelper.RIGHT: if (b == null) {
                                                        b = new Bundle();
                                                    }

                                                    b.putInt("idToUpdate", contactsAdapter.getCurrentList().get(viewHolder.getBindingAdapterPosition()).getNrTelefone());
                                                    b.putString("fragmentToLoad", "NewContactFragment");
                                                    requireParentFragment().requireParentFragment().getChildFragmentManager().setFragmentResult(requireParentFragment().getClass().getSimpleName(), b);
                            break;
                    }
                }
            });
        }

        itemTouchHelper.attachToRecyclerView(binding.recyclerView);

        if (binding.recyclerView.getAdapter() == contactsAdapter) {
            return;
        }

        binding.recyclerView.setAdapter(contactsAdapter);

        List<Contacto> contactos = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> entry : viewModel.getContactos().get(bundle.getString("id")).entrySet()) {
            contactos.add(new Contacto(Integer.parseInt(entry.getKey()), Boolean.parseBoolean(entry.getValue().get("contactoPrincipal").toString())));
        }

        contactsAdapter.submitList(contactos);
    }

    private void observarCollection(QuerySnapshot value, FirebaseFirestoreException error) {
        try {
            if (value.isEmpty()) {
                if (binding.recyclerView.getAdapter() != contactAdapter) {
                    binding.recyclerView.setAdapter(contactAdapter);
                }

                if (viewModel.getContactos().containsKey(bundle.getString("id"))) {
                    viewModel.getContactos().get(bundle.getString("id")).clear();
                }

                State currentState = contactAdapter.getCurrentList().get(0);

                if (currentState != State.Empty) {
                    contactAdapter.submitList(Collections.singletonList(State.Empty));
                }

                return;
            }

            Map<String, Map<String, Object>> map = new HashMap<>();

            for (DocumentSnapshot snapshot : value) {
                map.put(snapshot.getId(), snapshot.getData());
            }

            if (viewModel.getContactos().containsKey(bundle.getString("id"))) {
                if (map.equals(viewModel.getContactos().get(bundle.getString("id")))) {
                    return;
                }
            }

            viewModel.getContactos().put(bundle.getString("id"), map);

            List<Contacto> contactos = new ArrayList<>();

            for (Map.Entry<String, Map<String, Object>> entry : viewModel.getContactos().get(bundle.getString("id")).entrySet()) {
                contactos.add(new Contacto(Integer.parseInt(entry.getKey()), Boolean.parseBoolean(entry.getValue().get("contactoPrincipal").toString())));
            }

            if (binding.recyclerView.getAdapter() != contactsAdapter) {
                binding.recyclerView.setAdapter(contactsAdapter);
            }

            contactsAdapter.submitList(contactos);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        if (clipboardManager == null) {
            clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        }

        ContactsAdapter.ViewHolder holder = (ContactsAdapter.ViewHolder) binding.recyclerView.getChildViewHolder(view);
        String toastMessage = holder == null ? "Sem nada" : "Copiado para a área de transferência";

        if (holder != null) {
            ClipData clipData = ClipData.newPlainText(null, holder.getNrTelefone());
            clipboardManager.setPrimaryClip(clipData);
        }

        Toast.makeText(requireContext(), toastMessage, Toast.LENGTH_LONG).show();
        return true;
    }
}