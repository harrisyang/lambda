package testsupport.traits;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.monad.Monad;
import com.jnape.palatable.lambda.monoid.builtin.Present;
import com.jnape.palatable.traitor.traits.Trait;

import java.util.function.Function;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Id.id;
import static java.util.Arrays.asList;

public class MonadLaws<M extends Monad> implements Trait<Monad<?, M>> {

    @Override
    public void test(Monad<?, M> m) {
        Present.<String>present((x, y) -> x + "\n\t - " + y)
                .<Function<Monad<?, M>, Maybe<String>>>foldMap(f -> f.apply(m), asList(
                        this::testLeftIdentity,
                        this::testRightIdentity,
                        this::testAssociativity))
                .peek(s -> {
                    throw new AssertionError("The following Monad laws did not hold for instance of " + m.getClass() + ": \n\t - " + s);
                });
    }

    private Maybe<String> testLeftIdentity(Monad<?, M> m) {
        Object a = new Object();
        Fn1<Object, Monad<Object, M>> fn = id().andThen(m::pure);
        return m.pure(a).flatMap(fn).equals(fn.apply(a))
                ? nothing()
                : just("left identity (m.pure(a).flatMap(fn).equals(fn.apply(a)))");
    }

    private Maybe<String> testRightIdentity(Monad<?, M> m) {
        return m.flatMap(m::pure).equals(m)
                ? nothing()
                : just("right identity: (m.flatMap(m::pure).equals(m))");
    }

    private Maybe<String> testAssociativity(Monad<?, M> m) {
        Fn1<Object, Monad<Object, M>> f = constantly(m.pure(new Object()));
        Function<Object, Monad<Object, M>> g = constantly(m.pure(new Object()));
        return m.flatMap(f).flatMap(g).equals(m.flatMap(a -> f.apply(a).flatMap(g)))
                ? nothing()
                : just("associativity: (m.flatMap(f).flatMap(g).equals(m.flatMap(a -> f.apply(a).flatMap(g))))");
    }
}
