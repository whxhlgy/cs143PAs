import java.io.PrintStream;
import java.util.*;

/**
 * This class may be used to contain the semantic information such as
 * the inheritance graph. You may use it or not as you like: it is only
 * here to provide a container for the supplied methods.
 */
class ClassTable {
    private int semantErrors;
    private PrintStream errorStream;

    class InheritanceGraph {
        Map<AbstractSymbol, Set<AbstractSymbol>> parents = new HashMap<AbstractSymbol, Set<AbstractSymbol>>();
        Set<AbstractSymbol> visited = new HashSet<AbstractSymbol>();
        Set<AbstractSymbol> stack = new HashSet<AbstractSymbol>();

        Map<AbstractSymbol, class_c> classes = new HashMap<AbstractSymbol, class_c>();

        public InheritanceGraph(Classes cls) {
            List<AbstractSymbol> basicClasses = Arrays.asList(
                TreeConstants.Object_, TreeConstants.IO, TreeConstants.Str, 
                TreeConstants.Int, TreeConstants.Bool, TreeConstants.SELF_TYPE);
            for (Enumeration e = cls.getElements(); e.hasMoreElements();) {
                Object n = e.nextElement();
                class_c c = (class_c) n;
                if (basicClasses.contains(c.getName())) {
                    semantError(c).println("Redefinition of basic class " + c.getName());;
                }
                if (classes.containsKey(c.getName())) {
                    semantError(c).println("Class " + c.getName() + " multiply defined");
                }
                classes.put(c.getName(), c);
            }
            // check if there is a Main class
            if (!classes.containsKey(TreeConstants.Main)) {
                semantError().println("Class Main is not defined.");
            }
        }

        public AbstractSymbol getLUB(AbstractSymbol a, AbstractSymbol b) {
            Set<AbstractSymbol> s1 = new HashSet<AbstractSymbol>();
            Set<AbstractSymbol> s2 = new HashSet<AbstractSymbol>();
            LUBUtil(a, s1);
            LUBUtil(b, s2);
            // find all s in s1 that in-degree = 0
            Map<AbstractSymbol, Integer> degrees = new HashMap<AbstractSymbol, Integer>();
            for (AbstractSymbol s : s2) {
                if (s1.contains(s)) {
                    degrees.put(s, 0);
                }
            }
            for (AbstractSymbol s : degrees.keySet()) {
                for (AbstractSymbol p : getSup(s)) {
                    if (degrees.containsKey(p)) {
                        degrees.put(p, degrees.get(p) + 1);
                    }
                }
            }
            for (AbstractSymbol s : degrees.keySet()) {
                if (degrees.get(s) == 0) {
                    return s;
                }
            }
            return TreeConstants.Object_;
        }
        /**
         * get all ancestors of AbstractSymbol a
         */
        public void LUBUtil(AbstractSymbol a, Set<AbstractSymbol> s) {
            for (AbstractSymbol p : getSup(a)) {
                LUBUtil(p, s);
            }
            s.add(a);
        }

        public Set<AbstractSymbol> getSup(AbstractSymbol c) {
            return parents.get(c) == null ? new HashSet<AbstractSymbol>() : parents.get(c);
        }

        void addClass(class_c c) {
            classes.put(c.getName(), c);
            // add all methods inside the class
            for (Enumeration f = c.features.getElements(); f.hasMoreElements(); ) {
                Feature feature = (Feature) f.nextElement();
                if (feature instanceof method) {
                    method method = (method) feature;
                    addMethod(c.getName(), method);
                }
            }
        }
        boolean hasClass(AbstractSymbol name) {
            return classes.containsKey(name);
        }
        class_c getClass(AbstractSymbol name) {
            return classes.get(name);
        }

        void addEdge(AbstractSymbol c, AbstractSymbol p) {
            // System.out.println("Add edge: " + c + " -> " + p);
            if (!parents.containsKey(c)) {
                parents.put(c, new HashSet<AbstractSymbol>());
            }
            parents.get(c).add(p);
        }

        void reportCycle(AbstractSymbol clz) {
            semantError(classes.get(clz))
                .println("Class " + clz + ", or an ancestor of " + clz + ", is involved in an inheritance cycle.");
        }

        boolean checkCycle() {
            for (AbstractSymbol clz : parents.keySet()) {
                if (!visited.contains(clz)) {
                    if (hasCycleUtil(clz)) {
                        reportCycle(clz);
                        return true;
                    }
                }
            }
            return false;
        }

        boolean hasCycleUtil(AbstractSymbol clz) {
            if (stack.contains(clz)) {
                return true;
            }
            stack.add(clz);
            if (parents.containsKey(clz)) {
                for (AbstractSymbol p : parents.get(clz)) {
                    if (hasCycleUtil(p)) {
                        return true;
                    }
                }
            }
            stack.remove(clz);
            visited.add(clz);
            return false;

        }
    }

    private InheritanceGraph graph;

    private SymbolTable objectEnv = new SymbolTable();

    class MethodId {
        private AbstractSymbol type;
        private AbstractSymbol name;

        public MethodId(AbstractSymbol t, AbstractSymbol n) {
            type = t;
            name = n;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            MethodId other = (MethodId) obj;
            if (type == null) {
                if (other.type != null) {
                    return false;
                }
            } else if (!type.equals(other.type)) {
                return false;
            }
            if (name == null) {
                if (other.name != null) {
                    return false;
                }
            } else if (!name.equals(other.name)) {
                return false;
            }
            return true;
        }
        @Override
        public int hashCode() {
            int result = 17;
            result = 31 * result + (type != null ? type.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            return result;
        }
    }

    private Map<MethodId, method> methodEnv = new HashMap<MethodId, method>();

    private class_c currentClass;

    public class_c getCurrentClass() {
        return currentClass;
    }
    public void setCurrentClass(class_c c) {
        this.currentClass = c;
    }
       

    /**
     * Some Utility Methods
     */

    // if a conforms to b return true
    public boolean conform(AbstractSymbol a, AbstractSymbol b) {
        // process SELF_TYPE
        a = a.equals(TreeConstants.SELF_TYPE) ? currentClass.name : a;
        b = b.equals(TreeConstants.SELF_TYPE) ? currentClass.name : b;
        if (a.equals(b)) {
            return true;
        }
        AbstractSymbol p = a;
        if (!p.equals(TreeConstants.Object_)) {
            for (AbstractSymbol parent : graph.getSup(p)) {
                if (conform(parent, b)) {
                    return true;
                }
            }
        }
        return false;
    }

    public AbstractSymbol getLUB(AbstractSymbol a, AbstractSymbol b) {
        return graph.getLUB(a, b);
    }

    public void errorPrint(String s) {
        semantError(currentClass).println(s);
    }

    public void enterScope() {
        objectEnv.enterScope();
    }

    public void exitScope() {
        objectEnv.exitScope();
    }
    
    public void addObj(AbstractSymbol id, AbstractSymbol type) {
        objectEnv.addId(id, type);
    }

    public AbstractSymbol lookupObj(AbstractSymbol id) {
        return (AbstractSymbol) objectEnv.lookup(id);
    }
    public boolean hasClass(AbstractSymbol clz) {
        if (clz.equals(TreeConstants.SELF_TYPE)) {
            clz = currentClass.name;
        }
        return graph.hasClass(clz);
    }

    public void addMethod(AbstractSymbol type, method m) {
        MethodId key = new MethodId(type, m.name);
        checkRedefine(type, m);
        methodEnv.put(key, m);
    }

    void checkRedefine(AbstractSymbol type, method m) {
        // find if this method redefine a method from parent class
        method original = lookupMethod(type, m.name);
        class_c clz = graph.getClass(type);
        if (original == null) {
            return;
        }
        // check if the redefined method conform the original one
        List<AbstractSymbol> formals1 = new ArrayList<AbstractSymbol>();
        List<AbstractSymbol> formals2 = new ArrayList<AbstractSymbol>();
        for (Enumeration f1 = m.formals.getElements(); f1.hasMoreElements();) {
            formals1.add(((formalc)f1.nextElement()).type_decl);
        }
        for (Enumeration f2 = original.formals.getElements(); f2.hasMoreElements();) {
            formals2.add(((formalc)f2.nextElement()).type_decl);
        }
        // do check:
        if (formals1.size() != formals2.size()) {
            semantError(clz).println("In redefined method " + m.name + 
                ", number of arguments is different from original method " + original.name + ".");
            return;
        }
        for (int i = 0; i < formals1.size(); i++) {
            if (!formals1.get(i).equals(formals2.get(i))) {
                semantError(clz).println("In redefined method " + m.name + 
                    ", argument type is different from original method " + original.name + ".");
                return;
            }
        }
        if (m.return_type != original.return_type) {
            semantError(clz).println("In redefined method " + m.name + 
                ", return type is different from original method " + original.name + ".");
            return;
        }
    }

    public method lookupMethod(AbstractSymbol type, AbstractSymbol name) {
        MethodId key = new MethodId(type, name);
        if (methodEnv.containsKey(key)) {
            return (method) methodEnv.get(key);
        }
        for (AbstractSymbol sup : graph.getSup(type)) {
            method m = lookupMethod(sup, name);
            if (m != null) {
                return m;
            }
        }
        return null;
    }
    /**
     * Get all attributes in the inheritance hierarchy of a class
     */
    public void addAllAttributes(AbstractSymbol type) {
        Set<AbstractSymbol> visited = new HashSet<AbstractSymbol>();
        Set<AbstractSymbol> added = new HashSet<AbstractSymbol>();
        addAllAttributesUtils(type, visited, added);
    }

    private void addAllAttributesUtils(AbstractSymbol type, 
        Set<AbstractSymbol> visited, Set<AbstractSymbol> added) {

        if (visited.contains(type)) {
            return;
        }
        for (AbstractSymbol sup : graph.getSup(type)) {
            addAllAttributesUtils(sup, visited, added);
        }
        class_c c = graph.getClass(type);
        Features features = c.features;
        for (Enumeration e = features.getElements(); e.hasMoreElements(); ) {
            Feature feature = (Feature) e.nextElement();
            if (feature instanceof attr) {
                attr attr = (attr) feature;
                if (attr.name.equals(TreeConstants.self)) {
                    // attr cannot be named self
                    errorPrint("'self' cannot be the name of an attribute");
                } else {
                    if(!added.add(attr.name)) {
                        errorPrint("Attribute " + attr.name + " is an attribute of an inherited class");
                    } else {
                        addObj(attr.name, attr.type_decl);
                    }
                }
            }
        }
        visited.add(type);
    }


    /**
     * Creates data structures representing basic Cool classes (Object,
     * IO, Int, Bool, String). Please note: as is this method does not
     * do anything useful; you will need to edit it to make if do what
     * you want.
     */
    private void installBasicClasses() {
        AbstractSymbol filename = AbstractTable.stringtable.addString("<basic class>");

        // The following demonstrates how to create dummy parse trees to
        // refer to basic Cool classes. There's no need for method
        // bodies -- these are already built into the runtime system.

        // IMPORTANT: The results of the following expressions are
        // stored in local variables. You will want to do something
        // with those variables at the end of this method to make this
        // code meaningful.

        // The Object class has no parent class. Its methods are
        // cool_abort() : Object aborts the program
        // type_name() : Str returns a string representation
        // of class name
        // copy() : SELF_TYPE returns a copy of the object

        class_c Object_class = new class_c(0,
                TreeConstants.Object_,
                TreeConstants.No_class,
                new Features(0)
                        .appendElement(new method(0,
                                TreeConstants.cool_abort,
                                new Formals(0),
                                TreeConstants.Object_,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.type_name,
                                new Formals(0),
                                TreeConstants.Str,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.copy,
                                new Formals(0),
                                TreeConstants.SELF_TYPE,
                                new no_expr(0))),
                filename);

        // The IO class inherits from Object. Its methods are
        // out_string(Str) : SELF_TYPE writes a string to the output
        // out_int(Int) : SELF_TYPE " an int " " "
        // in_string() : Str reads a string from the input
        // in_int() : Int " an int " " "

        class_c IO_class = new class_c(0,
                TreeConstants.IO,
                TreeConstants.Object_,
                new Features(0)
                        .appendElement(new method(0,
                                TreeConstants.out_string,
                                new Formals(0)
                                        .appendElement(new formalc(0,
                                                TreeConstants.arg,
                                                TreeConstants.Str)),
                                TreeConstants.SELF_TYPE,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.out_int,
                                new Formals(0)
                                        .appendElement(new formalc(0,
                                                TreeConstants.arg,
                                                TreeConstants.Int)),
                                TreeConstants.SELF_TYPE,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.in_string,
                                new Formals(0),
                                TreeConstants.Str,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.in_int,
                                new Formals(0),
                                TreeConstants.Int,
                                new no_expr(0))),
                filename);

        // The Int class has no methods and only a single attribute, the
        // "val" for the integer.

        class_c Int_class = new class_c(0,
                TreeConstants.Int,
                TreeConstants.Object_,
                new Features(0)
                        .appendElement(new attr(0,
                                TreeConstants.val,
                                TreeConstants.prim_slot,
                                new no_expr(0))),
                filename);

        // Bool also has only the "val" slot.
        class_c Bool_class = new class_c(0,
                TreeConstants.Bool,
                TreeConstants.Object_,
                new Features(0)
                        .appendElement(new attr(0,
                                TreeConstants.val,
                                TreeConstants.prim_slot,
                                new no_expr(0))),
                filename);

        // The class Str has a number of slots and operations:
        // val the length of the string
        // str_field the string itself
        // length() : Int returns length of the string
        // concat(arg: Str) : Str performs string concatenation
        // substr(arg: Int, arg2: Int): Str substring selection

        class_c Str_class = new class_c(0,
                TreeConstants.Str,
                TreeConstants.Object_,
                new Features(0)
                        .appendElement(new attr(0,
                                TreeConstants.val,
                                TreeConstants.Int,
                                new no_expr(0)))
                        .appendElement(new attr(0,
                                TreeConstants.str_field,
                                TreeConstants.prim_slot,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.length,
                                new Formals(0),
                                TreeConstants.Int,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.concat,
                                new Formals(0)
                                        .appendElement(new formalc(0,
                                                TreeConstants.arg,
                                                TreeConstants.Str)),
                                TreeConstants.Str,
                                new no_expr(0)))
                        .appendElement(new method(0,
                                TreeConstants.substr,
                                new Formals(0)
                                        .appendElement(new formalc(0,
                                                TreeConstants.arg,
                                                TreeConstants.Int))
                                        .appendElement(new formalc(0,
                                                TreeConstants.arg2,
                                                TreeConstants.Int)),
                                TreeConstants.Str,
                                new no_expr(0))),
                filename);

        /*
         * Do somethind with Object_class, IO_class, Int_class,
         * Bool_class, and Str_class here
         */
        // fill in the graph
        graph.addClass(Object_class);
        graph.addClass(IO_class);
        graph.addClass(Int_class);
        graph.addClass(Bool_class);
        graph.addClass(Str_class);
        graph.addEdge(IO_class.getName(), Object_class.getName());
        graph.addEdge(Int_class.getName(), Object_class.getName());
        graph.addEdge(Bool_class.getName(), Object_class.getName());
        graph.addEdge(Str_class.getName(), Object_class.getName());
    }

    public ClassTable(Classes cls) {
        semantErrors = 0;
        errorStream = System.err;

        /* fill this in */
        graph = new InheritanceGraph(cls);
        installBasicClasses();
        Set<AbstractSymbol> basic = new HashSet<AbstractSymbol>();
        basic.add(TreeConstants.Str);
        basic.add(TreeConstants.Int);
        basic.add(TreeConstants.Bool);
        for (Enumeration e = cls.getElements(); e.hasMoreElements();) {
            Object n = e.nextElement();
            class_c c = (class_c) n;
            AbstractSymbol parent = c.getParent();
            if (!graph.hasClass(parent)) {
                semantError(c).println("Class " + c.getName() + "inherits from an undefined class " + parent);
            }
            if (basic.contains(parent)) {
                semantError(c).println("Class " + c.getName() + " cannot inherits from basic class " + parent);
            }
            graph.addEdge(((class_c) n).getName(), ((class_c) n).getParent());
        }
        // do some checks
        graph.checkCycle();
    }

    /**
     * Prints line number and file name of the given class.
     *
     * Also increments semantic error count.
     *
     * @param c the class
     * @return a print stream to which the rest of the error message is
     *         to be printed.
     *
     */
    public PrintStream semantError(class_c c) {
        return semantError(c.getFilename(), c);
    }

    /**
     * Prints the file name and the line number of the given tree node.
     *
     * Also increments semantic error count.
     *
     * @param filename the file name
     * @param t        the tree node
     * @return a print stream to which the rest of the error message is
     *         to be printed.
     *
     */
    public PrintStream semantError(AbstractSymbol filename, TreeNode t) {
        errorStream.print(filename + ":" + t.getLineNumber() + ": ");
        return semantError();
    }

    /**
     * Increments semantic error count and returns the print stream for
     * error messages.
     *
     * @return a print stream to which the error message is
     *         to be printed.
     *
     */
    public PrintStream semantError() {
        semantErrors++;
        return errorStream;
    }

    /** Returns true if there are any static semantic errors. */
    public boolean errors() {
        return semantErrors != 0;
    }
}
