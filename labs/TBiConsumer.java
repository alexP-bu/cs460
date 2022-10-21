import java.io.IOException;
import java.util.function.BiConsumer;

@FunctionalInterface
public interface TBiConsumer<T, U> extends BiConsumer<T, U> {
    default void accept(T arg0, U arg1) {
        try {
            acc(arg0, arg1);
        } catch (final IOException e){
            e.printStackTrace();
        }
    }

    void acc(T arg0, U arg1) throws IOException;
}