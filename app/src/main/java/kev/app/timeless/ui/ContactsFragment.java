package kev.app.timeless.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

public class ContactsFragment extends DaggerFragment implements View.OnClickListener, State<ContactsFragment.State>, View.OnLongClickListener {
    private Observer<List<User>> observer;
    private LayoutDefaultBinding binding;
    private Bundle bundle, b;
    private ContactAdapter contactAdapter;
    private ContactsAdapter contactsAdapter;
    private State state;
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
        }, this, this);
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
        state = null;
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

        if (TextUtils.isEmpty(result.getString("id"))) {
            return;
        }

        if (state == null) {
            state = viewModel.getContactos().containsKey(result.getString("id")) ? State.Loaded : State.Loading;
        }

        contactAdapter.submitList(Collections.singletonList(state));

        switch (state) {
            case Loading: obterContactos(result);
                break;
            case Loaded: onLoad();
                break;
        }
    }

    private void observarDocument(DocumentSnapshot documentSnapshot, FirebaseFirestoreException exception) {
        try {
            if (!documentSnapshot.exists()) {
                viewModel.getContactos().remove(documentSnapshot.getId());
                return;
            }

            viewModel.getContactos().put(documentSnapshot.getId(), null);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void obterContactos(Bundle result) {
        disposable = viewModel.getService().getBarbeariaService().obterContactos(result.getString("id"))
                .timeout(5, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess(stringObjectMap -> viewModel.getContactos().put(result.getString("id"), stringObjectMap))
                .doAfterSuccess(stringObjectMap -> state = State.Loaded)
                .doOnError(throwable -> state = State.Error)
                .subscribe(stringObjectMap -> onLoad(), throwable -> contactAdapter.submitList(Collections.singletonList(state)));
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

    @Override
    public State value() {
        return state;
    }

    public void onLoad() {
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

        if (itemTouchHelper == null) {
            itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                @Override
                public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                    return false;
                }

                @Override
                public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                    switch (direction) {
                        case ItemTouchHelper.LEFT: // delete contact
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

        for (Map<String, Object> map : Objects.requireNonNull(viewModel.getContactos().get(bundle.getString("id")))) {
            Contacto contacto = new Contacto();

            for (Map.Entry<String, Object> entry : map.entrySet()) {
                switch (entry.getKey()) {
                    case "nrTelefone": contacto.setNrTelefone(Integer.parseInt(entry.getValue().toString()));
                        break;
                    case "posicao": contacto.setPosicao(Integer.parseInt(entry.getValue().toString()));
                        break;
                }
            }

            contactos.add(contacto);
        }

        contactsAdapter.submitList(contactos);
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

    public enum State {
        Loading,
        Error,
        Loaded
    }
}