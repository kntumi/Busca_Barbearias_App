package kev.app.timeless.api.Barbearia;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

import io.reactivex.Maybe;
import kev.app.timeless.model.Result;

public interface BarbeariaService {
    Maybe<Map<String, Map<String, Object>>> buscarBarbearias(double latitude, double longitude);
    Maybe<Boolean> inserirBarbearia(Map<String, ?> map);
    Maybe<Boolean> editarNome(String nome, String id);
    Maybe<Boolean> editarEstado(Boolean aBoolean, String id);
    Maybe<Boolean> inserirHorarioPorDia(Integer diaSemana, Map<String, Map<String, Map<String, Float>>> map, String id);
    Maybe<Map<String, Map<String, Object>>> obterServiços(String id);
    Maybe<Boolean> removerContacto(String id, String nrContacto);
    Maybe<Map<String, Map<String, Object>>> obterTiposServiços(String id, String idServiço);
    Maybe<Map<String, Map<String, Object>>> obterSubServiços (String id, String idServiço, String idTipoServiço);
    Result<Map<String, Object>> obterEstabelecimento(String id);
    Maybe<Map<String, Map<String, Object>>> obterContactos(String id);
    Task<QuerySnapshot> obterHorario(String id);
    Maybe<Boolean> obterContacto(String id, String nrContacto);
    Maybe<Boolean> removerHorario(String id, int dia);
    Maybe<Boolean> inserirHorario(String id, int dia, Map<String, Double> horario);
    Maybe<Boolean> inserirServiço(String id, Map<String, String> map);
    Maybe<Boolean> inserirTipoServiço(String id, String idServiço, Map<String, String> map);
    Maybe<Boolean> removerServiço(String id, String idServiço);
    Maybe<Boolean> removerTipoServiço(String id, String idServiço, String idTipoServiço);
    Maybe<Boolean> inserirSubServiço(String id, String idServiço, String idTipoServiço, Map<String, Object> map);
    Maybe<Boolean> inserirEstado(String id, boolean estado);
    Maybe<Boolean> inserirContacto (String id, String nrContacto, Map<String, Object> map);
    Maybe<Boolean> editarSubServiço(String id, String idServiço, String idTipoServiço, String idSubServico, Map<String, Object> map);
    Maybe<Boolean> removerSubServiço(String id, String idServiço, String idTipoServiço, String idSubServico);
}