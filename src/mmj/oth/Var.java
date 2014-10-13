package mmj.oth;

import java.util.Map;

public class Var {
    Name n;
    Type t;

    public Var(final Type t, final Name n) {
        this.n = n;
        this.t = t;
    }

    public Var subst(final Map<Var, Var> subst) {
        final Var sub = subst.get(this);
        return sub == null ? this : sub;
    }

    @Override
    public int hashCode() {
        return n.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return n.equals(((Var)obj).n) && t.equals(((Var)obj).t);
    }

    @Override
    public String toString() {
        return n.toString();
    }
}