//#terminating
/*
 * Date: 10.11.2013
 * Author: heizmann@informatik.uni-freiburg.de
 *
 */
var x,y: int;

procedure main()
modifies x, y;
{
  while (x>0) {
    call x := decrease(x);
  }
}

procedure decrease(a: int) returns (res: int)
{
  if (*) {
    res := a - 1;
  } else {
    res := a - 7;
  }
    
}