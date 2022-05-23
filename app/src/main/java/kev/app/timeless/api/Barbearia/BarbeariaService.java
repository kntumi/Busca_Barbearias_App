package kev.app.timeless.api.Barbearia;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Map;

import io.reactivex.Maybe;

public interface BarbeariaService {
    Maybe<Map<String, Map<String, Object>>> buscarBarbearias(LatLng latLng);
    Maybe<Boolean> inserirBarbearia(Map<String, ?> map);
    Maybe<Boolean> editarNome(String nome, String id);
    Maybe<Boolean> editarEstado(Boolean aBoolean, String id);
    Maybe<Boolean> inserirHorarioPorDia(Integer diaSemana, Map<String, Map<String, Map<String, Float>>> map, String id);
    Maybe<Map<String, Map<String, Object>>> obterServiços(String id);
    Maybe<Map<String, Map<String, Object>>> obterTiposServiços(String id, String idServiço);
    Maybe<Map<String, Map<String, Object>>> obterSubServiços (String id, String idServiço, String idTipoServiço);
    Maybe<Map<String, Object>> obterEstabelecimento(String id);
    Maybe<List<Map<String, Object>>> obterContactos(String id);
    Maybe<Map<String, Map<String, Double>>> obterHorário(String id);
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