let x = "X-global";
{
  function showX() {
    console.log(x);
  }

  showX();
  x = "X-block";
  showX();
}

// noinspection JSUnusedGlobalSymbols
let y = "Y-global";
{
  const showY = () => console.log(y);
  showY();
  let y = "Y-block";
  showY();
}	
