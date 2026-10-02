import java.util.*;
import java.util.function.*;

record Promocion(String nombre, Predicate<Producto> aplicaA, DoubleUnaryOperator ajuste) {
    double precioCon(Producto p) {
        return aplicaA.test(p) ? ajuste.applyAsDouble(p.precio()) : p.precio();
    }
}

public class Main {
    static DoubleUnaryOperator porcentaje(double pct) { return precio -> precio * (1 - pct / 100); }
    static DoubleUnaryOperator montoFijo(double monto) { return precio -> Math.max(0, precio - monto); }

    public static void main(String[] args) {
        List<Promocion> promociones = List.of(
            new Promocion("Tecno 10 %", p -> p.categoria().equals("tecnología"), porcentaje(10)),
            new Promocion("Bono $50.000", p -> p.precio() >= 200_000, montoFijo(50_000)),
            new Promocion("Liquidación", p -> p.stock() > 100, porcentaje(30)));

        Supplier<Promocion> sinPromocion =
            () -> new Promocion("Sin promoción", p -> true, DoubleUnaryOperator.identity());
        BiConsumer<Producto, Promocion> imprimir = (p, promo) -> System.out.printf(
            "%-17s %-14s $%,11.0f → $%,11.0f%n", p.nombre(), promo.nombre(), p.precio(),
            promo.precioCon(p));

        ToDoubleBiFunction<Producto, Promocion> ahorro = (p, promo) -> p.precio() - promo.precioCon(p);

        double ahorroTotal = 0;
        for (Producto p : Catalogo.muestra()) {
            if (!p.disponible()) continue;
            BinaryOperator<Promocion> masConveniente =
                BinaryOperator.minBy(Comparator.comparingDouble(promo -> promo.precioCon(p)));
            Promocion mejor = sinPromocion.get();
            for (Promocion promo : promociones) mejor = masConveniente.apply(mejor, promo);
            imprimir.accept(p, mejor);
            ahorroTotal += ahorro.applyAsDouble(p, mejor);
        }
        System.out.printf("Ahorro total para el cliente: $%,.0f%n", ahorroTotal);
    }
}
