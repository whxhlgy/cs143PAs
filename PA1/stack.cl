(*
 *  CS164 Fall 94
 *
 *  Programming Assignment 1
 *    Implementation of a simple stack machine.
 *
 *  Skeleton file
 *)

-- class List {
--   item : StackCommand;
--   next : List;
--   init (i: StackCommand, n: List) : List {
--     {
--       item <- i;
--       next <- n;
--     }
--   };
-- }

class StackCommand {
  v: String;
  next : StackCommand;

  -- methods
  value() : String { v };
  next() : StackCommand { next };
  setNext(n: StackCommand) : StackCommand { next <- n };

  init(s: String, n: StackCommand) : StackCommand {
    {
      v <- s;
      next <- n;
      self;
    }
  };

  -- abstract methods
  eval() : StackCommand { { abort(); self; } };
};

class IntCommand inherits StackCommand {
  eval() : StackCommand {
    self
  };
};
class PlusCommand inherits StackCommand {
  eval() : StackCommand {
    let n1 : StackCommand <- next(),
        n2 : StackCommand <- next().next(),
        v1 : Int <- (new A2I).a2i(n1.value()),
        v2 : Int <- (new A2I).a2i(n2.value())
    in {
      (new IntCommand).init((new A2I).i2a(v1 + v2), n2.next());
    }
  };
};
class SwapCommand inherits StackCommand {
  eval() : StackCommand {
    let n1 : StackCommand <- next(),
        n2 : StackCommand <- next().next(),
        n3 : StackCommand <- n2.next()
    in {
      n2.setNext(n1);
      n1.setNext(n3);
      n2;
    }
  };
};

class Main inherits IO {

  main() : Object {
    {
      let command: String,
          top: StackCommand
          -- top: StackCommand <- (new StackCommand).init(0, nil)
      in {
        out_string(">");
        command <- in_string();
        while (not command = "x") loop {
          out_string(command);

          (* do something *)
          if command = "e" then 
            if (isvoid top) then
              out_string("Stack is empty\n")
            else
              top <- top.eval()
            fi
          else if command = "d" then
          let stack : StackCommand <- top in
          {
            -- display the stack
            out_string("\n");
            while not isvoid stack loop {
              out_string(stack.value());
              out_string("\n");
              stack <- stack.next();
            } pool;
          }
          else if command = "s" then
            top <- (new SwapCommand).init(command, top)
          else if command = "+" then
            top <- (new PlusCommand).init(command, top)
          else 
            top <- (new IntCommand).init(command, top)
          fi fi fi fi;

          out_string("\n");
          out_string(">");
          command <- in_string();
        } pool;
        out_string("x\n");
      };
    }
  };

};
