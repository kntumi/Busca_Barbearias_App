package kev.app.timeless.api.Barbearia;

import android.text.TextUtils;

import com.firebase.geofire.GeoFireUtils;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQueryBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.util.DateTime;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Maybe;
import kev.app.timeless.model.Result;

public class Barbearia {
    public static Maybe<Boolean> removerSubServiço(String id, String idServiço, String idTipoServiço, String idSubServico, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .document(idTipoServiço)
                .collection("subservicos")
                .document(idSubServico)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> editarSubServiço(String id, String idServiço, String idTipoServiço, String idSubServico, Map<String, Object> map, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .document(idTipoServiço)
                .collection("subservicos")
                .document(idSubServico)
                .set(map)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }
    public static Maybe<Boolean> inserirEstado (String id, boolean estado, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> {
            firestore.collection("Barbearia")
                    .document(id)
                    .set(new HashMap<>(Collections.singletonMap("estado", estado)), SetOptions.mergeFields("estado"))
                    .addOnCompleteListener(task -> {
                        if (!emitter.isDisposed()) {
                            emitter.onSuccess(task.isSuccessful());
                        }
                    })
                    .addOnFailureListener(e -> {
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    public static Maybe<Boolean> inserirSubServiço(String id, String idServiço, String idTipoServiço, Map<String, Object> map, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .document(idTipoServiço)
                .collection("subservicos")
                .add(map)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    System.out.println("sick");
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> removerTipoServiço(String id, String idServiço, String idTipoServiço, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .document(idTipoServiço)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> inserirTipoServiço(String id, String idServiço, Map<String, String> map, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .add(map)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> inserirContacto(String id, String nrContacto, Map<String, Object> map, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("contactos")
                .document(nrContacto)
                .set(map)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> removerServiço(String id, String idServiço, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> inserirServiço(String id, Map<String, String> map, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .add(map)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Map<String, Map<String, Object>>> obterSubServiços (String id, String idServiço, String idTipoServiço, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .document(idTipoServiço)
                .collection("subservicos")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Map<String, Object>> subServiços = new HashMap<>();

                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        subServiços.put(documentSnapshot.getId(), documentSnapshot.getData());
                    }

                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(subServiços);
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> inserirHorario(String id, int dia, Map<String, Double> horario, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("horario")
                .document(String.valueOf(dia))
                .set(horario)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> removerHorario(String id, int dia, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("horario")
                .document(String.valueOf(dia))
                .delete()
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Task<QuerySnapshot> obterHorario(String id, FirebaseFirestore firestore) {
        return firestore.collection("Barbearia").document(id).collection("horario").get();
    }

    public static Maybe<Map<String, Map<String, Object>>> obterContactos(String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("contactos")
                .get()
                .addOnCompleteListener(task -> {
                    Map<String, Map<String, Object>> map = new HashMap<>();

                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                        map.put(documentSnapshot.getId(), documentSnapshot.getData());
                    }

                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(map);
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Result<Map<String, Object>> obterEstabelecimento(String id, HttpRequestFactory requestFactory, Gson gson) {
        if (TextUtils.isEmpty(id)) {
            return new Result.Error<>(new NullPointerException());
        }

        LinkedTreeMap<?, ?> map;

        try {
            map = gson.fromJson(requestFactory.buildGetRequest(new GenericUrl("https://firestore.googleapis.com/v1/projects/rupertt-d42df/databases/(default)/documents/Barbearia/".concat(id))).execute().parseAsString(), LinkedTreeMap.class);
        } catch (IOException e) {
            return new Result.Error<>(e);
        }

        HashMap<String, Object> hashMap = new HashMap<>();

        for (Object key : map.keySet()) {
            switch (String.valueOf(key)) {
                case "fields" : LinkedTreeMap<?, ?> treeMap = (LinkedTreeMap<?, ?>) map.get(key);

                                for (Map.Entry<?, ?> entry : treeMap.entrySet()) {
                                    LinkedTreeMap<?, ?> linkedHashMap = (LinkedTreeMap<?, ?>) entry.getValue();

                                    for (Map.Entry<?, ?> stringEntry : linkedHashMap.entrySet()) {
                                        hashMap.put(String.valueOf(entry.getKey()), stringEntry.getValue());
                                    }
                                }

                                break;

                case "updateTime": hashMap.put(String.valueOf(key), DateTime.parseRfc3339(String.valueOf(map.get(key))).getValue());
                                break;
            }
        }

        return new Result.Success<>(hashMap);
    }

    public static Maybe<Map<String, Object>> obterEstabelecimento (String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia").document(id)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!emitter.isDisposed()) {
                            Map<String, Object> map = task.getResult().getData();

                            if (map == null) {
                                map = new HashMap<>();
                            }

                            emitter.onSuccess(map);
                        }
                    }

                    if (task.getException() != null) {
                        if (!emitter.isDisposed()) {
                            emitter.onError(task.getException());
                        }
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
        }));
    }

    public static Maybe<Map<String, Map<String, Object>>> obterServiços(String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia").document(id)
                .collection("servicos")
                .get()
                .addOnCompleteListener(task -> {
                    Map<String, Map<String, Object>> map = new HashMap<>();

                    if (task.isSuccessful()) {
                        for (DocumentSnapshot documentSnapshot : task.getResult().getDocuments()) {
                            map.put(documentSnapshot.getId(), documentSnapshot.getData());
                        }
                    }

                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(map);
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> inserirHorarioPorDia(Integer diaSemana, Map<String, Map<String, Map<String, Float>>> map, String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia").document(id)
                .update("horario", map)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> editarEstado (Boolean estado, String id, FirebaseFirestore firestore){
        return Maybe.create(emitter -> firestore.collection("Barbearia").document(id).update("estado", estado));
    }

    public static Maybe<Boolean> editarNome(String nome, String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .update("nome", nome)
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> removerContacto(String id, String nrContacto, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("contactos")
                .document(nrContacto)
                .delete()
                .addOnCompleteListener(task -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(task.isSuccessful());
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Maybe<Boolean> obterContacto(String id, String nrContacto, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore
                .collection("Barbearia")
                .document(id)
                .collection("contactos")
                .document(nrContacto)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(documentSnapshot.getData() != null);
                    }
                }).addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }

    public static Query getPlaceQuery(FirebaseFirestore firestore, String startHash, String endHash) {
        return firestore.collection("Barbearia").orderBy("hash").startAt(startHash).endAt(endHash);
    }

    public static Maybe<Map<String, Map<String, Object>>> buscarBarbearias(double latitude, double longitude, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> {
            try {
                List<GeoQueryBounds> bounds = GeoFireUtils.getGeoHashQueryBounds(new GeoLocation(latitude, longitude), 0.5 * 1000);

                final List<Task<QuerySnapshot>> tasks = new ArrayList<>();

                for (GeoQueryBounds b : bounds) {
                    Query q = firestore.collection("Barbearia").orderBy("hash").startAt(b.startHash).endAt(b.endHash);

                    try {
                        tasks.add(q.get());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                Tasks.whenAllComplete(tasks)
                        .addOnSuccessListener(taskList -> {
                            if (!emitter.isDisposed()) {
                                Map<String, Map<String, Object>> estabelecimentos = new HashMap<>();

                                for (Task<QuerySnapshot> task : tasks) {
                                    for (DocumentSnapshot documentSnapshot : task.getResult()) {
                                        estabelecimentos.put(documentSnapshot.getId(), documentSnapshot.getData());
                                    }
                                }

                                if (!emitter.isDisposed()) {
                                    emitter.onSuccess(estabelecimentos);
                                }
                            }
                        }).addOnFailureListener(e -> {
                            if (!emitter.isDisposed()) {
                                emitter.onError(e);
                            }
                        });
            } catch (Exception e) {
                if (!emitter.isDisposed()) {
                    emitter.onError(e);
                }
            }
        });
    }

    public static Maybe<Boolean> inserirBarbearia (Map<String, ?> map, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia").add(map).addOnFailureListener(e -> {
            if (!emitter.isDisposed()) {
                emitter.onError(e);
            }
        }));
    }

    public static  Maybe<Map<String, Object>> buscarBarbeariaPorID(String id, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> {
            firestore.collection("Barbearia").document(id).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            if (!emitter.isDisposed()) {
                                emitter.onSuccess(documentSnapshot.getData());
                            }

                            return;
                        }

                        if (!emitter.isDisposed()) {
                            emitter.onError(new NullPointerException());
                        }
                    }).addOnFailureListener(e -> {
                        if (!emitter.isDisposed()) {
                            emitter.onError(e);
                        }
                    });
        });
    }

    public static Maybe<Map<String, Map<String, Object>>> obterTiposServiços(String id, String idServiço, FirebaseFirestore firestore) {
        return Maybe.create(emitter -> firestore.collection("Barbearia")
                .document(id)
                .collection("servicos")
                .document(idServiço)
                .collection("tipos")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Map<String, Object>> tiposServiços = new HashMap<>();

                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        tiposServiços.put(documentSnapshot.getId(), documentSnapshot.getData());
                    }

                    if (!emitter.isDisposed()) {
                        emitter.onSuccess(tiposServiços);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!emitter.isDisposed()) {
                        emitter.onError(e);
                    }
                }));
    }
}