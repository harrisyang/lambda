package com.jnape.palatable.lambda.lens;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functor.builtin.Const;
import com.jnape.palatable.lambda.functor.builtin.Identity;
import com.jnape.palatable.traitor.annotations.TestTraits;
import com.jnape.palatable.traitor.runners.Traits;
import org.junit.Test;
import org.junit.runner.RunWith;
import testsupport.EqualityAwareLens;
import testsupport.traits.ApplicativeLaws;
import testsupport.traits.FunctorLaws;
import testsupport.traits.MonadLaws;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.lens.Lens.both;
import static com.jnape.palatable.lambda.lens.Lens.lens;
import static com.jnape.palatable.lambda.lens.Lens.simpleLens;
import static com.jnape.palatable.lambda.lens.functions.Set.set;
import static com.jnape.palatable.lambda.lens.functions.View.view;
import static java.lang.Integer.parseInt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

@RunWith(Traits.class)
public class LensTest {

    private static final Lens<Map<String, List<String>>, Map<String, Set<Integer>>, List<String>, Set<Integer>> EARLIER_LENS = lens(m -> m.get("foo"), (m, s) -> singletonMap("foo", s));
    private static final Lens<List<String>, Set<Integer>, String, Integer>                                      LENS         = lens(xs -> xs.get(0), (xs, i) -> singleton(i));

    @TestTraits({FunctorLaws.class, ApplicativeLaws.class, MonadLaws.class})
    public Lens<Map<String, Integer>, List<Integer>, Integer, String> testSubject() {
        return new EqualityAwareLens<>(emptyMap(), lens(m -> m.get("foo"), (m, s) -> singletonList(m.get(s))));
    }

    @Test
    public void setsUnderIdentity() {
        Set<Integer> ints = LENS.<Identity, Identity<Set<Integer>>, Identity<Integer>>apply(s -> new Identity<>(s.length()), asList("foo", "bar", "baz")).runIdentity();
        assertEquals(singleton(3), ints);
    }

    @Test
    public void viewsUnderConst() {
        Integer i = LENS.<Const<Integer, ?>, Const<Integer, Set<Integer>>, Const<Integer, Integer>>apply(s -> new Const<>(s.length()), asList("foo", "bar", "baz")).runConst();
        assertEquals((Integer) 3, i);
    }

    @Test
    public void fix() {
        Fn1<String, Const<Integer, Integer>> fn = s -> new Const<>(s.length());
        List<String> s = singletonList("foo");

        Integer fixedLensResult = LENS.<Const<Integer, ?>, Const<Integer, Set<Integer>>, Const<Integer, Integer>>fix().apply(fn, s).runConst();
        Integer unfixedLensResult = LENS.<Const<Integer, ?>, Const<Integer, Set<Integer>>, Const<Integer, Integer>>apply(fn, s).runConst();

        assertEquals(unfixedLensResult, fixedLensResult);
    }

    @Test
    public void mapsIndividuallyOverParameters() {
        Lens<String, Boolean, Character, Integer> lens = lens(s -> s.charAt(0), (s, b) -> s.length() == b);
        Lens<Maybe<String>, Maybe<Boolean>, Maybe<Character>, Maybe<Integer>> theGambit = lens
                .mapS((Maybe<String> maybeS) -> maybeS.orElse(""))
                .mapT(Maybe::maybe)
                .mapA(Maybe::maybe)
                .mapB((Maybe<Integer> maybeI) -> maybeI.orElse(-1));

        Lens.Fixed<Maybe<String>, Maybe<Boolean>, Maybe<Character>, Maybe<Integer>, Identity, Identity<Maybe<Boolean>>, Identity<Maybe<Integer>>> fixed = theGambit.fix();
        assertEquals(just(true), fixed.apply(maybeC -> new Identity<>(maybeC.fmap(c -> parseInt(Character.toString(c)))), just("321")).runIdentity());
    }

    @Test
    public void composition() {
        Map<String, List<String>> map = singletonMap("foo", asList("one", "two", "three"));
        assertEquals("one", view(LENS.compose(EARLIER_LENS), map));
        assertEquals(singletonMap("foo", singleton(1)), set(LENS.compose(EARLIER_LENS), 1, map));
    }

    @Test
    public void andThenComposesInReverse() {
        Map<String, List<String>> map = singletonMap("foo", asList("one", "two", "three"));
        assertEquals("one", view(EARLIER_LENS.andThen(LENS), map));
        assertEquals(singletonMap("foo", singleton(1)), set(EARLIER_LENS.andThen(LENS), 1, map));
    }

    @Test
    public void bothSplitsFocusBetweenLenses() {
        Lens<String, String, Character, Character> firstChar = simpleLens(s -> s.charAt(0), (s, c) -> c + s.substring(1));
        Lens<String, String, Integer, Integer> length = simpleLens(String::length, (s, k) -> s.substring(0, k));
        Lens<String, String, Tuple2<Character, Integer>, Tuple2<Character, Integer>> both = both(firstChar, length);

        assertEquals(tuple('a', 3), view(both, "abc"));
        assertEquals("zb", set(both, tuple('z', 2), "abc"));
    }
}