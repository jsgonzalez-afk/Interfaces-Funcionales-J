import java.util.ArrayList;
import java.util.List;
import java.util.function.*;

@FunctionalInterface
interface Regla<T> {
    List<String> validar(T valor);

    default Regla<T> y(Regla<? super T> otra) {
        return valor -> {
            List<String> errores = new ArrayList<>(validar(valor));
            errores.addAll(otra.validar(valor));
            return errores;
        };
    }

    static <T> Regla<T> exigir(Predicate<? super T> condicion, String mensaje) {
        return valor -> condicion.test(valor) ? List.of() : List.of(mensaje);
    }

    static <T, C> Regla<T> campo(Function<? super T, ? extends C> extractor, Regla<? super C> regla) {
        return valor -> regla.validar(extractor.apply(valor));
    }
}

record Registro(String usuario, String correo, int edad) { }

public class Main {
    public static void main(String[] args) {
        Regla<String> usuarioTexto = Regla.<String>exigir(s -> !s.isBlank(), "usuario: obligatorio")
            .y(Regla.exigir(s -> s.length() >= 4, "usuario: mínimo 4 caracteres"));
        Regla<Registro> usuarioValido = Regla.campo(Registro::usuario, usuarioTexto);
        Regla<Registro> correoValido = Regla.exigir(
            r -> r.correo().matches("[^@\\s]+@[^@\\s]+\\.[a-z]{2,}"), "correo: formato inválido");
        Regla<Registro> edadValida = Regla.exigir(r -> r.edad() >= 14, "edad: mínimo 14 años");
        Regla<Registro> todas = usuarioValido.y(correoValido).y(edadValida);

        List<Registro> registros = List.of(new Registro("camila_r", "camila@correo.co", 19),
            new Registro("", "sin-arroba", 12), new Registro("ana", "ana@correo", 15));
        for (Registro r : registros) {
            List<String> errores = todas.validar(r);
            String usuario = r.usuario().isEmpty() ? "(vacío)" : r.usuario();
            System.out.printf("%-9s %-10s %s%n", errores.isEmpty() ? "VÁLIDO" : "INVÁLIDO", usuario, errores);
        }
    }
}
