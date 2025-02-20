<%@ page pageEncoding="windows-1250" %>

<h1>Forms</h1>

<section>
    <h2>Form controls</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-group">
                    <label for="exampleFormControlInput1">Email address</label>
                    <input type="email" class="form-control" id="exampleFormControlInput1" placeholder="name@example.com">
                </div>
                <div class="form-group">
                    <label for="exampleFormControlSelect1">Example select</label>
                    <select class="form-control" id="exampleFormControlSelect1">
                        <option>1</option>
                        <option>2</option>
                        <option>3</option>
                        <option>4</option>
                        <option>5</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="exampleFormControlSelect2">Example multiple select</label>
                    <select multiple class="form-control" id="exampleFormControlSelect2">
                        <option>1</option>
                        <option>2</option>
                        <option>3</option>
                        <option>4</option>
                        <option>5</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="exampleFormControlTextarea1">Example textarea</label>
                    <textarea class="form-control" id="exampleFormControlTextarea1" rows="3"></textarea>
                </div>
                <div class="form-group">
                    <label for="exampleFormControlFile1">Example file input</label>
                    <input type="file" class="form-control-file" id="exampleFormControlFile1">
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;exampleFormControlInput1&quot;&gt;Email address&lt;/label&gt;
        &lt;input type=&quot;email&quot; class=&quot;form-control&quot; id=&quot;exampleFormControlInput1&quot; placeholder=&quot;name@example.com&quot;&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;exampleFormControlSelect1&quot;&gt;Example select&lt;/label&gt;
        &lt;select class=&quot;form-control&quot; id=&quot;exampleFormControlSelect1&quot;&gt;
          &lt;option&gt;1&lt;/option&gt;
          &lt;option&gt;2&lt;/option&gt;
          &lt;option&gt;3&lt;/option&gt;
          &lt;option&gt;4&lt;/option&gt;
          &lt;option&gt;5&lt;/option&gt;
        &lt;/select&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;exampleFormControlSelect2&quot;&gt;Example multiple select&lt;/label&gt;
        &lt;select multiple class=&quot;form-control&quot; id=&quot;exampleFormControlSelect2&quot;&gt;
          &lt;option&gt;1&lt;/option&gt;
          &lt;option&gt;2&lt;/option&gt;
          &lt;option&gt;3&lt;/option&gt;
          &lt;option&gt;4&lt;/option&gt;
          &lt;option&gt;5&lt;/option&gt;
        &lt;/select&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;exampleFormControlTextarea1&quot;&gt;Example textarea&lt;/label&gt;
        &lt;textarea class=&quot;form-control&quot; id=&quot;exampleFormControlTextarea1&quot; rows=&quot;3&quot;&gt;&lt;/textarea&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Sizing</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="form-group">
                <input class="form-control form-control-lg" type="text" placeholder=".form-control-lg">
            </div>
            <div class="form-group">
                <input class="form-control" type="text" placeholder="Default input">
            </div>
            <div class="form-group">
                <input class="form-control form-control-sm" type="text" placeholder=".form-control-sm">
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;input class=&quot;form-control form-control-lg&quot; type=&quot;text&quot; placeholder=&quot;.form-control-lg&quot;&gt;
    &lt;input class=&quot;form-control&quot; type=&quot;text&quot; placeholder=&quot;Default input&quot;&gt;
    &lt;input class=&quot;form-control form-control-sm&quot; type=&quot;text&quot; placeholder=&quot;.form-control-sm&quot;&gt;
    </code>
  </pre>

    <h3>Readonly</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <input class="form-control" type="text" placeholder="Readonly input here..." readonly>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;input class=&quot;form-control&quot; type=&quot;text&quot; placeholder=&quot;Readonly input here...&quot; readonly&gt;
    </code>
  </pre>

    <h3>Readonly plain text</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-group row">
                    <label for="staticEmail" class="col-sm-2 col-form-label">Email</label>
                    <div class="col-sm-10">
                        <input type="text" readonly class="form-control-plaintext" id="staticEmail" value="email@example.com">
                    </div>
                </div>
                <div class="form-group row">
                    <label for="inputPassword" class="col-sm-2 col-form-label">Password</label>
                    <div class="col-sm-10">
                        <input type="password" class="form-control" id="inputPassword" placeholder="Password">
                    </div>
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-group row&quot;&gt;
        &lt;label for=&quot;staticEmail&quot; class=&quot;col-sm-2 col-form-label&quot;&gt;Email&lt;/label&gt;
        &lt;div class=&quot;col-sm-10&quot;&gt;
          &lt;input type=&quot;text&quot; readonly class=&quot;form-control-plaintext&quot; id=&quot;staticEmail&quot; value=&quot;email@example.com&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group row&quot;&gt;
        &lt;label for=&quot;inputPassword&quot; class=&quot;col-sm-2 col-form-label&quot;&gt;Password&lt;/label&gt;
        &lt;div class=&quot;col-sm-10&quot;&gt;
          &lt;input type=&quot;password&quot; class=&quot;form-control&quot; id=&quot;inputPassword&quot; placeholder=&quot;Password&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <form class="form-inline">
                <div class="form-group mb-2">
                    <label for="staticEmail2" class="sr-only">Email</label>
                    <input type="text" readonly class="form-control-plaintext" id="staticEmail2" value="email@example.com">
                </div>
                <div class="form-group mx-sm-3 mb-2">
                    <label for="inputPassword2" class="sr-only">Password</label>
                    <input type="password" class="form-control" id="inputPassword2" placeholder="Password">
                </div>
                <button type="submit" class="btn btn-primary mb-2">Confirm identity</button>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form class=&quot;form-inline&quot;&gt;
      &lt;div class=&quot;form-group mb-2&quot;&gt;
        &lt;label for=&quot;staticEmail2&quot; class=&quot;sr-only&quot;&gt;Email&lt;/label&gt;
        &lt;input type=&quot;text&quot; readonly class=&quot;form-control-plaintext&quot; id=&quot;staticEmail2&quot; value=&quot;email@example.com&quot;&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group mx-sm-3 mb-2&quot;&gt;
        &lt;label for=&quot;inputPassword2&quot; class=&quot;sr-only&quot;&gt;Password&lt;/label&gt;
        &lt;input type=&quot;password&quot; class=&quot;form-control&quot; id=&quot;inputPassword2&quot; placeholder=&quot;Password&quot;&gt;
      &lt;/div&gt;
      &lt;button type=&quot;submit&quot; class=&quot;btn btn-primary mb-2&quot;&gt;Confirm identity&lt;/button&gt;
    &lt;/form&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Range Inputs</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-group">
                    <label for="formControlRange">Example Range input</label>
                    <input type="range" class="form-control-range" id="formControlRange">
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;formControlRange&quot;&gt;Example Range input&lt;/label&gt;
        &lt;input type=&quot;range&quot; class=&quot;form-control-range&quot; id=&quot;formControlRange&quot;&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Checkboxes and radios</h2>
    <h3>Default (stacked)</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" id="defaultCheck1">
                <label class="form-check-label" for="defaultCheck1">
                    Default checkbox
                </label>
            </div>
            <div class="form-check">
                <input class="form-check-input" type="checkbox" value="" id="defaultCheck2" disabled>
                <label class="form-check-label" for="defaultCheck2">
                    Disabled checkbox
                </label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;form-check&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; value=&quot;&quot; id=&quot;defaultCheck1&quot;&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;defaultCheck1&quot;&gt;
        Default checkbox
      &lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; value=&quot;&quot; id=&quot;defaultCheck2&quot; disabled&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;defaultCheck2&quot;&gt;
        Disabled checkbox
      &lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <div class="form-check">
                <input class="form-check-input" type="radio" name="exampleRadios" id="exampleRadios1" value="option1" checked>
                <label class="form-check-label" for="exampleRadios1">
                    Default radio
                </label>
            </div>
            <div class="form-check">
                <input class="form-check-input" type="radio" name="exampleRadios" id="exampleRadios2" value="option2">
                <label class="form-check-label" for="exampleRadios2">
                    Second default radio
                </label>
            </div>
            <div class="form-check">
                <input class="form-check-input" type="radio" name="exampleRadios" id="exampleRadios3" value="option3" disabled>
                <label class="form-check-label" for="exampleRadios3">
                    Disabled radio
                </label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;form-check&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;exampleRadios&quot; id=&quot;exampleRadios1&quot; value=&quot;option1&quot; checked&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;exampleRadios1&quot;&gt;
        Default radio
      &lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;exampleRadios&quot; id=&quot;exampleRadios2&quot; value=&quot;option2&quot;&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;exampleRadios2&quot;&gt;
        Second default radio
      &lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;exampleRadios&quot; id=&quot;exampleRadios3&quot; value=&quot;option3&quot; disabled&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;exampleRadios3&quot;&gt;
        Disabled radio
      &lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <h3>Inline</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" id="inlineCheckbox1" value="option1">
                <label class="form-check-label" for="inlineCheckbox1">1</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" id="inlineCheckbox2" value="option2">
                <label class="form-check-label" for="inlineCheckbox2">2</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="checkbox" id="inlineCheckbox3" value="option3" disabled>
                <label class="form-check-label" for="inlineCheckbox3">3 (disabled)</label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;form-check form-check-inline&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;inlineCheckbox1&quot; value=&quot;option1&quot;&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;inlineCheckbox1&quot;&gt;1&lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check form-check-inline&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;inlineCheckbox2&quot; value=&quot;option2&quot;&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;inlineCheckbox2&quot;&gt;2&lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check form-check-inline&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;inlineCheckbox3&quot; value=&quot;option3&quot; disabled&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;inlineCheckbox3&quot;&gt;3 (disabled)&lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="inlineRadioOptions" id="inlineRadio1" value="option1">
                <label class="form-check-label" for="inlineRadio1">1</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="inlineRadioOptions" id="inlineRadio2" value="option2">
                <label class="form-check-label" for="inlineRadio2">2</label>
            </div>
            <div class="form-check form-check-inline">
                <input class="form-check-input" type="radio" name="inlineRadioOptions" id="inlineRadio3" value="option3" disabled>
                <label class="form-check-label" for="inlineRadio3">3 (disabled)</label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;form-check form-check-inline&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;inlineRadioOptions&quot; id=&quot;inlineRadio1&quot; value=&quot;option1&quot;&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;inlineRadio1&quot;&gt;1&lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check form-check-inline&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;inlineRadioOptions&quot; id=&quot;inlineRadio2&quot; value=&quot;option2&quot;&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;inlineRadio2&quot;&gt;2&lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;form-check form-check-inline&quot;&gt;
      &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;inlineRadioOptions&quot; id=&quot;inlineRadio3&quot; value=&quot;option3&quot; disabled&gt;
      &lt;label class=&quot;form-check-label&quot; for=&quot;inlineRadio3&quot;&gt;3 (disabled)&lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Layout</h2>
    <h3>Form groups</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-group">
                    <label for="formGroupExampleInput">Example label</label>
                    <input type="text" class="form-control" id="formGroupExampleInput" placeholder="Example input">
                </div>
                <div class="form-group">
                    <label for="formGroupExampleInput2">Another label</label>
                    <input type="text" class="form-control" id="formGroupExampleInput2" placeholder="Another input">
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;formGroupExampleInput&quot;&gt;Example label&lt;/label&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;formGroupExampleInput&quot; placeholder=&quot;Example input&quot;&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;formGroupExampleInput2&quot;&gt;Another label&lt;/label&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;formGroupExampleInput2&quot; placeholder=&quot;Another input&quot;&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Form row</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-row">
                    <div class="col">
                        <input type="text" class="form-control" placeholder="First name">
                    </div>
                    <div class="col">
                        <input type="text" class="form-control" placeholder="Last name">
                    </div>
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col&quot;&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; placeholder=&quot;First name&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col&quot;&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; placeholder=&quot;Last name&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Form grid</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="inputEmail4">Email</label>
                        <input type="email" class="form-control" id="inputEmail4" placeholder="Email">
                    </div>
                    <div class="form-group col-md-6">
                        <label for="inputPassword4">Password</label>
                        <input type="password" class="form-control" id="inputPassword4" placeholder="Password">
                    </div>
                </div>
                <div class="form-group">
                    <label for="inputAddress">Address</label>
                    <input type="text" class="form-control" id="inputAddress" placeholder="1234 Main St">
                </div>
                <div class="form-group">
                    <label for="inputAddress2">Address 2</label>
                    <input type="text" class="form-control" id="inputAddress2" placeholder="Apartment, studio, or floor">
                </div>
                <div class="form-row">
                    <div class="form-group col-md-6">
                        <label for="inputCity">City</label>
                        <input type="text" class="form-control" id="inputCity">
                    </div>
                    <div class="form-group col-md-4">
                        <label for="inputState">State</label>
                        <select id="inputState" class="form-control">
                            <option selected>Choose...</option>
                            <option>...</option>
                        </select>
                    </div>
                    <div class="form-group col-md-2">
                        <label for="inputZip">Zip</label>
                        <input type="text" class="form-control" id="inputZip">
                    </div>
                </div>
                <div class="form-group">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" id="gridCheck">
                        <label class="form-check-label" for="gridCheck">
                            Check me out
                        </label>
                    </div>
                </div>
                <button type="submit" class="btn btn-primary">Sign in</button>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;form-group col-md-6&quot;&gt;
          &lt;label for=&quot;inputEmail4&quot;&gt;Email&lt;/label&gt;
          &lt;input type=&quot;email&quot; class=&quot;form-control&quot; id=&quot;inputEmail4&quot; placeholder=&quot;Email&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;form-group col-md-6&quot;&gt;
          &lt;label for=&quot;inputPassword4&quot;&gt;Password&lt;/label&gt;
          &lt;input type=&quot;password&quot; class=&quot;form-control&quot; id=&quot;inputPassword4&quot; placeholder=&quot;Password&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;inputAddress&quot;&gt;Address&lt;/label&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inputAddress&quot; placeholder=&quot;1234 Main St&quot;&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;inputAddress2&quot;&gt;Address 2&lt;/label&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inputAddress2&quot; placeholder=&quot;Apartment, studio, or floor&quot;&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;form-group col-md-6&quot;&gt;
          &lt;label for=&quot;inputCity&quot;&gt;City&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inputCity&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;form-group col-md-4&quot;&gt;
          &lt;label for=&quot;inputState&quot;&gt;State&lt;/label&gt;
          &lt;select id=&quot;inputState&quot; class=&quot;form-control&quot;&gt;
            &lt;option selected&gt;Choose...&lt;/option&gt;
            &lt;option&gt;...&lt;/option&gt;
          &lt;/select&gt;
        &lt;/div&gt;
        &lt;div class=&quot;form-group col-md-2&quot;&gt;
          &lt;label for=&quot;inputZip&quot;&gt;Zip&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inputZip&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;div class=&quot;form-check&quot;&gt;
          &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;gridCheck&quot;&gt;
          &lt;label class=&quot;form-check-label&quot; for=&quot;gridCheck&quot;&gt;
            Check me out
          &lt;/label&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;button type=&quot;submit&quot; class=&quot;btn btn-primary&quot;&gt;Sign in&lt;/button&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Horizontal form</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-group row">
                    <label for="inputEmail3" class="col-sm-2 col-form-label">Email</label>
                    <div class="col-sm-10">
                        <input type="email" class="form-control" id="inputEmail3" placeholder="Email">
                    </div>
                </div>
                <div class="form-group row">
                    <label for="inputPassword3" class="col-sm-2 col-form-label">Password</label>
                    <div class="col-sm-10">
                        <input type="password" class="form-control" id="inputPassword3" placeholder="Password">
                    </div>
                </div>
                <fieldset class="form-group">
                    <div class="row">
                        <legend class="col-form-label col-sm-2 pt-0">Radios</legend>
                        <div class="col-sm-10">
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="gridRadios" id="gridRadios1" value="option1" checked>
                                <label class="form-check-label" for="gridRadios1">
                                    First radio
                                </label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="gridRadios" id="gridRadios2" value="option2">
                                <label class="form-check-label" for="gridRadios2">
                                    Second radio
                                </label>
                            </div>
                            <div class="form-check disabled">
                                <input class="form-check-input" type="radio" name="gridRadios" id="gridRadios3" value="option3" disabled>
                                <label class="form-check-label" for="gridRadios3">
                                    Third disabled radio
                                </label>
                            </div>
                        </div>
                    </div>
                </fieldset>
                <div class="form-group row">
                    <div class="col-sm-2">Checkbox</div>
                    <div class="col-sm-10">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="gridCheck1">
                            <label class="form-check-label" for="gridCheck1">
                                Example checkbox
                            </label>
                        </div>
                    </div>
                </div>
                <div class="form-group row">
                    <div class="col-sm-10">
                        <button type="submit" class="btn btn-primary">Sign in</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-group row&quot;&gt;
        &lt;label for=&quot;inputEmail3&quot; class=&quot;col-sm-2 col-form-label&quot;&gt;Email&lt;/label&gt;
        &lt;div class=&quot;col-sm-10&quot;&gt;
          &lt;input type=&quot;email&quot; class=&quot;form-control&quot; id=&quot;inputEmail3&quot; placeholder=&quot;Email&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group row&quot;&gt;
        &lt;label for=&quot;inputPassword3&quot; class=&quot;col-sm-2 col-form-label&quot;&gt;Password&lt;/label&gt;
        &lt;div class=&quot;col-sm-10&quot;&gt;
          &lt;input type=&quot;password&quot; class=&quot;form-control&quot; id=&quot;inputPassword3&quot; placeholder=&quot;Password&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;fieldset class=&quot;form-group&quot;&gt;
        &lt;div class=&quot;row&quot;&gt;
          &lt;legend class=&quot;col-form-label col-sm-2 pt-0&quot;&gt;Radios&lt;/legend&gt;
          &lt;div class=&quot;col-sm-10&quot;&gt;
            &lt;div class=&quot;form-check&quot;&gt;
              &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;gridRadios&quot; id=&quot;gridRadios1&quot; value=&quot;option1&quot; checked&gt;
              &lt;label class=&quot;form-check-label&quot; for=&quot;gridRadios1&quot;&gt;
                First radio
              &lt;/label&gt;
            &lt;/div&gt;
            &lt;div class=&quot;form-check&quot;&gt;
              &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;gridRadios&quot; id=&quot;gridRadios2&quot; value=&quot;option2&quot;&gt;
              &lt;label class=&quot;form-check-label&quot; for=&quot;gridRadios2&quot;&gt;
                Second radio
              &lt;/label&gt;
            &lt;/div&gt;
            &lt;div class=&quot;form-check disabled&quot;&gt;
              &lt;input class=&quot;form-check-input&quot; type=&quot;radio&quot; name=&quot;gridRadios&quot; id=&quot;gridRadios3&quot; value=&quot;option3&quot; disabled&gt;
              &lt;label class=&quot;form-check-label&quot; for=&quot;gridRadios3&quot;&gt;
                Third disabled radio
              &lt;/label&gt;
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/fieldset&gt;
      &lt;div class=&quot;form-group row&quot;&gt;
        &lt;div class=&quot;col-sm-2&quot;&gt;Checkbox&lt;/div&gt;
        &lt;div class=&quot;col-sm-10&quot;&gt;
          &lt;div class=&quot;form-check&quot;&gt;
            &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;gridCheck1&quot;&gt;
            &lt;label class=&quot;form-check-label&quot; for=&quot;gridCheck1&quot;&gt;
              Example checkbox
            &lt;/label&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group row&quot;&gt;
        &lt;div class=&quot;col-sm-10&quot;&gt;
          &lt;button type=&quot;submit&quot; class=&quot;btn btn-primary&quot;&gt;Sign in&lt;/button&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Auto-sizing</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-row align-items-center">
                    <div class="col-sm-3 my-1">
                        <label class="sr-only" for="inlineFormInputName">Name</label>
                        <input type="text" class="form-control" id="inlineFormInputName" placeholder="Jane Doe">
                    </div>
                    <div class="col-sm-3 my-1">
                        <label class="sr-only" for="inlineFormInputGroupUsername">Username</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <div class="input-group-text">@</div>
                            </div>
                            <input type="text" class="form-control" id="inlineFormInputGroupUsername" placeholder="Username">
                        </div>
                    </div>
                    <div class="col-auto my-1">
                        <div class="form-check">
                            <input class="form-check-input" type="checkbox" id="autoSizingCheck2">
                            <label class="form-check-label" for="autoSizingCheck2">
                                Remember me
                            </label>
                        </div>
                    </div>
                    <div class="col-auto my-1">
                        <button type="submit" class="btn btn-primary">Submit</button>
                    </div>
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-row align-items-center&quot;&gt;
        &lt;div class=&quot;col-sm-3 my-1&quot;&gt;
          &lt;label class=&quot;sr-only&quot; for=&quot;inlineFormInputName&quot;&gt;Name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inlineFormInputName&quot; placeholder=&quot;Jane Doe&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-sm-3 my-1&quot;&gt;
          &lt;label class=&quot;sr-only&quot; for=&quot;inlineFormInputGroupUsername&quot;&gt;Username&lt;/label&gt;
          &lt;div class=&quot;input-group&quot;&gt;
            &lt;div class=&quot;input-group-prepend&quot;&gt;
              &lt;div class=&quot;input-group-text&quot;&gt;@&lt;/div&gt;
            &lt;/div&gt;
            &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inlineFormInputGroupUsername&quot; placeholder=&quot;Username&quot;&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-auto my-1&quot;&gt;
          &lt;div class=&quot;form-check&quot;&gt;
            &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;autoSizingCheck2&quot;&gt;
            &lt;label class=&quot;form-check-label&quot; for=&quot;autoSizingCheck2&quot;&gt;
              Remember me
            &lt;/label&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-auto my-1&quot;&gt;
          &lt;button type=&quot;submit&quot; class=&quot;btn btn-primary&quot;&gt;Submit&lt;/button&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Inline forms</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form class="form-inline">
                <label class="sr-only" for="inlineFormInputName2">Name</label>
                <input type="text" class="form-control mb-2 mr-sm-2" id="inlineFormInputName2" placeholder="Jane Doe">

                <label class="sr-only" for="inlineFormInputGroupUsername2">Username</label>
                <div class="input-group mb-2 mr-sm-2">
                    <div class="input-group-prepend">
                        <div class="input-group-text">@</div>
                    </div>
                    <input type="text" class="form-control" id="inlineFormInputGroupUsername2" placeholder="Username">
                </div>

                <div class="form-check mb-2 mr-sm-2">
                    <input class="form-check-input" type="checkbox" id="inlineFormCheck">
                    <label class="form-check-label" for="inlineFormCheck">
                        Remember me
                    </label>
                </div>

                <button type="submit" class="btn btn-primary mb-2">Submit</button>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form class=&quot;form-inline&quot;&gt;
      &lt;label class=&quot;sr-only&quot; for=&quot;inlineFormInputName2&quot;&gt;Name&lt;/label&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control mb-2 mr-sm-2&quot; id=&quot;inlineFormInputName2&quot; placeholder=&quot;Jane Doe&quot;&gt;

      &lt;label class=&quot;sr-only&quot; for=&quot;inlineFormInputGroupUsername2&quot;&gt;Username&lt;/label&gt;
      &lt;div class=&quot;input-group mb-2 mr-sm-2&quot;&gt;
        &lt;div class=&quot;input-group-prepend&quot;&gt;
          &lt;div class=&quot;input-group-text&quot;&gt;@&lt;/div&gt;
        &lt;/div&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;inlineFormInputGroupUsername2&quot; placeholder=&quot;Username&quot;&gt;
      &lt;/div&gt;

      &lt;div class=&quot;form-check mb-2 mr-sm-2&quot;&gt;
        &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; id=&quot;inlineFormCheck&quot;&gt;
        &lt;label class=&quot;form-check-label&quot; for=&quot;inlineFormCheck&quot;&gt;
          Remember me
        &lt;/label&gt;
      &lt;/div&gt;

      &lt;button type=&quot;submit&quot; class=&quot;btn btn-primary mb-2&quot;&gt;Submit&lt;/button&gt;
    &lt;/form&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Help text</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <label for="inputPassword5">Password</label>
            <input type="password" id="inputPassword5" class="form-control" aria-describedby="passwordHelpBlock">
            <small id="passwordHelpBlock" class="form-text text-muted">
                Your password must be 8-20 characters long, contain letters and numbers, and must not contain spaces, special characters, or emoji.
            </small>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;label for=&quot;inputPassword5&quot;&gt;Password&lt;/label&gt;
    &lt;input type=&quot;password&quot; id=&quot;inputPassword5&quot; class=&quot;form-control&quot; aria-describedby=&quot;passwordHelpBlock&quot;&gt;
    &lt;small id=&quot;passwordHelpBlock&quot; class=&quot;form-text text-muted&quot;&gt;
      Your password must be 8-20 characters long, contain letters and numbers, and must not contain spaces, special characters, or emoji.
    &lt;/small&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <form class="form-inline">
                <div class="form-group">
                    <label for="inputPassword6">Password</label>
                    <input type="password" id="inputPassword6" class="form-control mx-sm-3" aria-describedby="passwordHelpInline">
                    <small id="passwordHelpInline" class="text-muted">
                        Must be 8-20 characters long.
                    </small>
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form class=&quot;form-inline&quot;&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;label for=&quot;inputPassword6&quot;&gt;Password&lt;/label&gt;
        &lt;input type=&quot;password&quot; id=&quot;inputPassword6&quot; class=&quot;form-control mx-sm-3&quot; aria-describedby=&quot;passwordHelpInline&quot;&gt;
        &lt;small id=&quot;passwordHelpInline&quot; class=&quot;text-muted&quot;&gt;
          Must be 8-20 characters long.
        &lt;/small&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Validation</h2>
    <h3>Custom styles</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <form class="needs-validation" novalidate>
                <div class="form-row">
                    <div class="col-md-4 mb-3">
                        <label for="validationCustom01">First name</label>
                        <input type="text" class="form-control" id="validationCustom01" placeholder="First name" value="Mark" required>
                        <div class="valid-feedback">
                            Looks good!
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label for="validationCustom02">Last name</label>
                        <input type="text" class="form-control" id="validationCustom02" placeholder="Last name" value="Otto" required>
                        <div class="valid-feedback">
                            Looks good!
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label for="validationCustomUsername">Username</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text" id="inputGroupPrepend">@</span>
                            </div>
                            <input type="text" class="form-control" id="validationCustomUsername" placeholder="Username" aria-describedby="inputGroupPrepend" required>
                            <div class="invalid-feedback">
                                Please choose a username.
                            </div>
                        </div>
                    </div>
                </div>
                <div class="form-row">
                    <div class="col-md-6 mb-3">
                        <label for="validationCustom03">City</label>
                        <input type="text" class="form-control" id="validationCustom03" placeholder="City" required>
                        <div class="invalid-feedback">
                            Please provide a valid city.
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label for="validationCustom04">State</label>
                        <input type="text" class="form-control" id="validationCustom04" placeholder="State" required>
                        <div class="invalid-feedback">
                            Please provide a valid state.
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label for="validationCustom05">Zip</label>
                        <input type="text" class="form-control" id="validationCustom05" placeholder="Zip" required>
                        <div class="invalid-feedback">
                            Please provide a valid zip.
                        </div>
                    </div>
                </div>
                <div class="form-group">
                    <div class="form-check">
                        <input class="form-check-input" type="checkbox" value="" id="invalidCheck" required>
                        <label class="form-check-label" for="invalidCheck">
                            Agree to terms and conditions
                        </label>
                        <div class="invalid-feedback">
                            You must agree before submitting.
                        </div>
                    </div>
                </div>
                <button class="btn btn-primary" type="submit">Submit form</button>
            </form>

            <script>
                // Example starter JavaScript for disabling form submissions if there are invalid fields
                (function() {
                    'use strict';
                    window.addEventListener('load', function() {
                        // Fetch all the forms we want to apply custom Bootstrap validation styles to
                        var forms = document.getElementsByClassName('needs-validation');
                        // Loop over them and prevent submission
                        var validation = Array.prototype.filter.call(forms, function(form) {
                            form.addEventListener('submit', function(event) {
                                if (form.checkValidity() === false) {
                                    event.preventDefault();
                                    event.stopPropagation();
                                }
                                form.classList.add('was-validated');
                            }, false);
                        });
                    }, false);
                })();
            </script>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form class=&quot;needs-validation&quot; novalidate&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationCustom01&quot;&gt;First name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationCustom01&quot; placeholder=&quot;First name&quot; value=&quot;Mark&quot; required&gt;
          &lt;div class=&quot;valid-feedback&quot;&gt;
            Looks good!
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationCustom02&quot;&gt;Last name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationCustom02&quot; placeholder=&quot;Last name&quot; value=&quot;Otto&quot; required&gt;
          &lt;div class=&quot;valid-feedback&quot;&gt;
            Looks good!
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationCustomUsername&quot;&gt;Username&lt;/label&gt;
          &lt;div class=&quot;input-group&quot;&gt;
            &lt;div class=&quot;input-group-prepend&quot;&gt;
              &lt;span class=&quot;input-group-text&quot; id=&quot;inputGroupPrepend&quot;&gt;@&lt;/span&gt;
            &lt;/div&gt;
            &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationCustomUsername&quot; placeholder=&quot;Username&quot; aria-describedby=&quot;inputGroupPrepend&quot; required&gt;
            &lt;div class=&quot;invalid-feedback&quot;&gt;
              Please choose a username.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col-md-6 mb-3&quot;&gt;
          &lt;label for=&quot;validationCustom03&quot;&gt;City&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationCustom03&quot; placeholder=&quot;City&quot; required&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            Please provide a valid city.
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-3 mb-3&quot;&gt;
          &lt;label for=&quot;validationCustom04&quot;&gt;State&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationCustom04&quot; placeholder=&quot;State&quot; required&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            Please provide a valid state.
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-3 mb-3&quot;&gt;
          &lt;label for=&quot;validationCustom05&quot;&gt;Zip&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationCustom05&quot; placeholder=&quot;Zip&quot; required&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            Please provide a valid zip.
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;div class=&quot;form-check&quot;&gt;
          &lt;input class=&quot;form-check-input&quot; type=&quot;checkbox&quot; value=&quot;&quot; id=&quot;invalidCheck&quot; required&gt;
          &lt;label class=&quot;form-check-label&quot; for=&quot;invalidCheck&quot;&gt;
            Agree to terms and conditions
          &lt;/label&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            You must agree before submitting.
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;button class=&quot;btn btn-primary&quot; type=&quot;submit&quot;&gt;Submit form&lt;/button&gt;
    &lt;/form&gt;

    &lt;script&gt;
    // Example starter JavaScript for disabling form submissions if there are invalid fields
    (function() {
      'use strict';
      window.addEventListener('load', function() {
        // Fetch all the forms we want to apply custom Bootstrap validation styles to
        var forms = document.getElementsByClassName('needs-validation');
        // Loop over them and prevent submission
        var validation = Array.prototype.filter.call(forms, function(form) {
          form.addEventListener('submit', function(event) {
            if (form.checkValidity() === false) {
              event.preventDefault();
              event.stopPropagation();
            }
            form.classList.add('was-validated');
          }, false);
        });
      }, false);
    })();
    &lt;/script&gt;
    </code>
  </pre>

    <h3>Server side</h3>
    <div class="row">
        <div class="col-12">
            <form>
                <div class="form-row">
                    <div class="col-md-4 mb-3">
                        <label for="validationServer01">First name</label>
                        <input type="text" class="form-control is-valid" id="validationServer01" placeholder="First name" value="Mark" required>
                        <div class="valid-feedback">
                            Looks good!
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label for="validationServer02">Last name</label>
                        <input type="text" class="form-control is-valid" id="validationServer02" placeholder="Last name" value="Otto" required>
                        <div class="valid-feedback">
                            Looks good!
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label for="validationServerUsername">Username</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text" id="inputGroupPrepend3">@</span>
                            </div>
                            <input type="text" class="form-control is-invalid" id="validationServerUsername" placeholder="Username" aria-describedby="inputGroupPrepend3" required>
                            <div class="invalid-feedback">
                                Please choose a username.
                            </div>
                        </div>
                    </div>
                </div>
                <div class="form-row">
                    <div class="col-md-6 mb-3">
                        <label for="validationServer03">City</label>
                        <input type="text" class="form-control is-invalid" id="validationServer03" placeholder="City" required>
                        <div class="invalid-feedback">
                            Please provide a valid city.
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label for="validationServer04">State</label>
                        <input type="text" class="form-control is-invalid" id="validationServer04" placeholder="State" required>
                        <div class="invalid-feedback">
                            Please provide a valid state.
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label for="validationServer05">Zip</label>
                        <input type="text" class="form-control is-invalid" id="validationServer05" placeholder="Zip" required>
                        <div class="invalid-feedback">
                            Please provide a valid zip.
                        </div>
                    </div>
                </div>
                <div class="form-group">
                    <div class="form-check">
                        <input class="form-check-input is-invalid" type="checkbox" value="" id="invalidCheck3" required>
                        <label class="form-check-label" for="invalidCheck3">
                            Agree to terms and conditions
                        </label>
                        <div class="invalid-feedback">
                            You must agree before submitting.
                        </div>
                    </div>
                </div>
                <button class="btn btn-primary" type="submit">Submit form</button>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationServer01&quot;&gt;First name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control is-valid&quot; id=&quot;validationServer01&quot; placeholder=&quot;First name&quot; value=&quot;Mark&quot; required&gt;
          &lt;div class=&quot;valid-feedback&quot;&gt;
            Looks good!
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationServer02&quot;&gt;Last name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control is-valid&quot; id=&quot;validationServer02&quot; placeholder=&quot;Last name&quot; value=&quot;Otto&quot; required&gt;
          &lt;div class=&quot;valid-feedback&quot;&gt;
            Looks good!
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationServerUsername&quot;&gt;Username&lt;/label&gt;
          &lt;div class=&quot;input-group&quot;&gt;
            &lt;div class=&quot;input-group-prepend&quot;&gt;
              &lt;span class=&quot;input-group-text&quot; id=&quot;inputGroupPrepend3&quot;&gt;@&lt;/span&gt;
            &lt;/div&gt;
            &lt;input type=&quot;text&quot; class=&quot;form-control is-invalid&quot; id=&quot;validationServerUsername&quot; placeholder=&quot;Username&quot; aria-describedby=&quot;inputGroupPrepend3&quot; required&gt;
            &lt;div class=&quot;invalid-feedback&quot;&gt;
              Please choose a username.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col-md-6 mb-3&quot;&gt;
          &lt;label for=&quot;validationServer03&quot;&gt;City&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control is-invalid&quot; id=&quot;validationServer03&quot; placeholder=&quot;City&quot; required&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            Please provide a valid city.
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-3 mb-3&quot;&gt;
          &lt;label for=&quot;validationServer04&quot;&gt;State&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control is-invalid&quot; id=&quot;validationServer04&quot; placeholder=&quot;State&quot; required&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            Please provide a valid state.
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-3 mb-3&quot;&gt;
          &lt;label for=&quot;validationServer05&quot;&gt;Zip&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control is-invalid&quot; id=&quot;validationServer05&quot; placeholder=&quot;Zip&quot; required&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            Please provide a valid zip.
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-group&quot;&gt;
        &lt;div class=&quot;form-check&quot;&gt;
          &lt;input class=&quot;form-check-input is-invalid&quot; type=&quot;checkbox&quot; value=&quot;&quot; id=&quot;invalidCheck3&quot; required&gt;
          &lt;label class=&quot;form-check-label&quot; for=&quot;invalidCheck3&quot;&gt;
            Agree to terms and conditions
          &lt;/label&gt;
          &lt;div class=&quot;invalid-feedback&quot;&gt;
            You must agree before submitting.
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;button class=&quot;btn btn-primary&quot; type=&quot;submit&quot;&gt;Submit form&lt;/button&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Supported elements</h3>
    <div class="row">
        <div class="col-12">
            <form class="was-validated">
                <div class="custom-control custom-checkbox mb-3">
                    <input type="checkbox" class="custom-control-input" id="customControlValidation1" required>
                    <label class="custom-control-label" for="customControlValidation1">Check this custom checkbox</label>
                    <div class="invalid-feedback">Example invalid feedback text</div>
                </div>

                <div class="custom-control custom-radio">
                    <input type="radio" class="custom-control-input" id="customControlValidation2" name="radio-stacked" required>
                    <label class="custom-control-label" for="customControlValidation2">Toggle this custom radio</label>
                </div>
                <div class="custom-control custom-radio mb-3">
                    <input type="radio" class="custom-control-input" id="customControlValidation3" name="radio-stacked" required>
                    <label class="custom-control-label" for="customControlValidation3">Or toggle this other custom radio</label>
                    <div class="invalid-feedback">More example invalid feedback text</div>
                </div>

                <div class="form-group">
                    <select class="custom-select" required>
                        <option value="">Open this select menu</option>
                        <option value="1">One</option>
                        <option value="2">Two</option>
                        <option value="3">Three</option>
                    </select>
                    <div class="invalid-feedback">Example invalid custom select feedback</div>
                </div>

                <div class="custom-file">
                    <input type="file" class="custom-file-input" id="validatedCustomFile" required>
                    <label class="custom-file-label" for="validatedCustomFile">Choose file...</label>
                    <div class="invalid-feedback">Example invalid custom file feedback</div>
                </div>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form class=&quot;was-validated&quot;&gt;
      &lt;div class=&quot;custom-control custom-checkbox mb-3&quot;&gt;
        &lt;input type=&quot;checkbox&quot; class=&quot;custom-control-input&quot; id=&quot;customControlValidation1&quot; required&gt;
        &lt;label class=&quot;custom-control-label&quot; for=&quot;customControlValidation1&quot;&gt;Check this custom checkbox&lt;/label&gt;
        &lt;div class=&quot;invalid-feedback&quot;&gt;Example invalid feedback text&lt;/div&gt;
      &lt;/div&gt;

      &lt;div class=&quot;custom-control custom-radio&quot;&gt;
        &lt;input type=&quot;radio&quot; class=&quot;custom-control-input&quot; id=&quot;customControlValidation2&quot; name=&quot;radio-stacked&quot; required&gt;
        &lt;label class=&quot;custom-control-label&quot; for=&quot;customControlValidation2&quot;&gt;Toggle this custom radio&lt;/label&gt;
      &lt;/div&gt;
      &lt;div class=&quot;custom-control custom-radio mb-3&quot;&gt;
        &lt;input type=&quot;radio&quot; class=&quot;custom-control-input&quot; id=&quot;customControlValidation3&quot; name=&quot;radio-stacked&quot; required&gt;
        &lt;label class=&quot;custom-control-label&quot; for=&quot;customControlValidation3&quot;&gt;Or toggle this other custom radio&lt;/label&gt;
        &lt;div class=&quot;invalid-feedback&quot;&gt;More example invalid feedback text&lt;/div&gt;
      &lt;/div&gt;

      &lt;div class=&quot;form-group&quot;&gt;
        &lt;select class=&quot;custom-select&quot; required&gt;
          &lt;option value=&quot;&quot;&gt;Open this select menu&lt;/option&gt;
          &lt;option value=&quot;1&quot;&gt;One&lt;/option&gt;
          &lt;option value=&quot;2&quot;&gt;Two&lt;/option&gt;
          &lt;option value=&quot;3&quot;&gt;Three&lt;/option&gt;
        &lt;/select&gt;
        &lt;div class=&quot;invalid-feedback&quot;&gt;Example invalid custom select feedback&lt;/div&gt;
      &lt;/div&gt;

      &lt;div class=&quot;custom-file&quot;&gt;
        &lt;input type=&quot;file&quot; class=&quot;custom-file-input&quot; id=&quot;validatedCustomFile&quot; required&gt;
        &lt;label class=&quot;custom-file-label&quot; for=&quot;validatedCustomFile&quot;&gt;Choose file...&lt;/label&gt;
        &lt;div class=&quot;invalid-feedback&quot;&gt;Example invalid custom file feedback&lt;/div&gt;
      &lt;/div&gt;
    &lt;/form&gt;
    </code>
  </pre>

    <h3>Tooltips</h3>
    <div class="row">
        <div class="col-12">
            <form class="needs-validation" novalidate>
                <div class="form-row">
                    <div class="col-md-4 mb-3">
                        <label for="validationTooltip01">First name</label>
                        <input type="text" class="form-control" id="validationTooltip01" placeholder="First name" value="Mark" required>
                        <div class="valid-tooltip">
                            Looks good!
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label for="validationTooltip02">Last name</label>
                        <input type="text" class="form-control" id="validationTooltip02" placeholder="Last name" value="Otto" required>
                        <div class="valid-tooltip">
                            Looks good!
                        </div>
                    </div>
                    <div class="col-md-4 mb-3">
                        <label for="validationTooltipUsername">Username</label>
                        <div class="input-group">
                            <div class="input-group-prepend">
                                <span class="input-group-text" id="validationTooltipUsernamePrepend">@</span>
                            </div>
                            <input type="text" class="form-control" id="validationTooltipUsername" placeholder="Username" aria-describedby="validationTooltipUsernamePrepend" required>
                            <div class="invalid-tooltip">
                                Please choose a unique and valid username.
                            </div>
                        </div>
                    </div>
                </div>
                <div class="form-row">
                    <div class="col-md-6 mb-3">
                        <label for="validationTooltip03">City</label>
                        <input type="text" class="form-control" id="validationTooltip03" placeholder="City" required>
                        <div class="invalid-tooltip">
                            Please provide a valid city.
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label for="validationTooltip04">State</label>
                        <input type="text" class="form-control" id="validationTooltip04" placeholder="State" required>
                        <div class="invalid-tooltip">
                            Please provide a valid state.
                        </div>
                    </div>
                    <div class="col-md-3 mb-3">
                        <label for="validationTooltip05">Zip</label>
                        <input type="text" class="form-control" id="validationTooltip05" placeholder="Zip" required>
                        <div class="invalid-tooltip">
                            Please provide a valid zip.
                        </div>
                    </div>
                </div>
                <button class="btn btn-primary" type="submit">Submit form</button>
            </form>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;form class=&quot;needs-validation&quot; novalidate&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationTooltip01&quot;&gt;First name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationTooltip01&quot; placeholder=&quot;First name&quot; value=&quot;Mark&quot; required&gt;
          &lt;div class=&quot;valid-tooltip&quot;&gt;
            Looks good!
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationTooltip02&quot;&gt;Last name&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationTooltip02&quot; placeholder=&quot;Last name&quot; value=&quot;Otto&quot; required&gt;
          &lt;div class=&quot;valid-tooltip&quot;&gt;
            Looks good!
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-4 mb-3&quot;&gt;
          &lt;label for=&quot;validationTooltipUsername&quot;&gt;Username&lt;/label&gt;
          &lt;div class=&quot;input-group&quot;&gt;
            &lt;div class=&quot;input-group-prepend&quot;&gt;
              &lt;span class=&quot;input-group-text&quot; id=&quot;validationTooltipUsernamePrepend&quot;&gt;@&lt;/span&gt;
            &lt;/div&gt;
            &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationTooltipUsername&quot; placeholder=&quot;Username&quot; aria-describedby=&quot;validationTooltipUsernamePrepend&quot; required&gt;
            &lt;div class=&quot;invalid-tooltip&quot;&gt;
              Please choose a unique and valid username.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;form-row&quot;&gt;
        &lt;div class=&quot;col-md-6 mb-3&quot;&gt;
          &lt;label for=&quot;validationTooltip03&quot;&gt;City&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationTooltip03&quot; placeholder=&quot;City&quot; required&gt;
          &lt;div class=&quot;invalid-tooltip&quot;&gt;
            Please provide a valid city.
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-3 mb-3&quot;&gt;
          &lt;label for=&quot;validationTooltip04&quot;&gt;State&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationTooltip04&quot; placeholder=&quot;State&quot; required&gt;
          &lt;div class=&quot;invalid-tooltip&quot;&gt;
            Please provide a valid state.
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col-md-3 mb-3&quot;&gt;
          &lt;label for=&quot;validationTooltip05&quot;&gt;Zip&lt;/label&gt;
          &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;validationTooltip05&quot; placeholder=&quot;Zip&quot; required&gt;
          &lt;div class=&quot;invalid-tooltip&quot;&gt;
            Please provide a valid zip.
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;button class=&quot;btn btn-primary&quot; type=&quot;submit&quot;&gt;Submit form&lt;/button&gt;
    &lt;/form&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Custom forms</h2>
    <h3>Checkboxes</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="custom-control custom-checkbox">
                <input type="checkbox" class="custom-control-input" id="customCheck1">
                <label class="custom-control-label" for="customCheck1">Check this custom checkbox</label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;custom-control custom-checkbox&quot;&gt;
      &lt;input type=&quot;checkbox&quot; class=&quot;custom-control-input&quot; id=&quot;customCheck1&quot;&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customCheck1&quot;&gt;Check this custom checkbox&lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <h3>Radios</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="custom-control custom-radio">
                <input type="radio" id="customRadio1" name="customRadio" class="custom-control-input">
                <label class="custom-control-label" for="customRadio1">Toggle this custom radio</label>
            </div>
            <div class="custom-control custom-radio">
                <input type="radio" id="customRadio2" name="customRadio" class="custom-control-input">
                <label class="custom-control-label" for="customRadio2">Or toggle this other custom radio</label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;custom-control custom-radio&quot;&gt;
      &lt;input type=&quot;radio&quot; id=&quot;customRadio1&quot; name=&quot;customRadio&quot; class=&quot;custom-control-input&quot;&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customRadio1&quot;&gt;Toggle this custom radio&lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;custom-control custom-radio&quot;&gt;
      &lt;input type=&quot;radio&quot; id=&quot;customRadio2&quot; name=&quot;customRadio&quot; class=&quot;custom-control-input&quot;&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customRadio2&quot;&gt;Or toggle this other custom radio&lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <h3>Inline</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="custom-control custom-radio custom-control-inline">
                <input type="radio" id="customRadioInline1" name="customRadioInline1" class="custom-control-input">
                <label class="custom-control-label" for="customRadioInline1">Toggle this custom radio</label>
            </div>
            <div class="custom-control custom-radio custom-control-inline">
                <input type="radio" id="customRadioInline2" name="customRadioInline1" class="custom-control-input">
                <label class="custom-control-label" for="customRadioInline2">Or toggle this other custom radio</label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;custom-control custom-radio custom-control-inline&quot;&gt;
      &lt;input type=&quot;radio&quot; id=&quot;customRadioInline1&quot; name=&quot;customRadioInline1&quot; class=&quot;custom-control-input&quot;&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customRadioInline1&quot;&gt;Toggle this custom radio&lt;/label&gt;
    &lt;/div&gt;
    &lt;div class=&quot;custom-control custom-radio custom-control-inline&quot;&gt;
      &lt;input type=&quot;radio&quot; id=&quot;customRadioInline2&quot; name=&quot;customRadioInline1&quot; class=&quot;custom-control-input&quot;&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customRadioInline2&quot;&gt;Or toggle this other custom radio&lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <h3>Disabled</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="custom-control custom-checkbox">
                <input type="checkbox" class="custom-control-input" id="customCheckDisabled" disabled>
                <label class="custom-control-label" for="customCheckDisabled">Check this custom checkbox</label>
            </div>

            <div class="custom-control custom-radio">
                <input type="radio" id="radio3" name="radioDisabled" id="customRadioDisabled" class="custom-control-input" disabled>
                <label class="custom-control-label" for="customRadioDisabled">Toggle this custom radio</label>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;custom-control custom-checkbox&quot;&gt;
      &lt;input type=&quot;checkbox&quot; class=&quot;custom-control-input&quot; id=&quot;customCheckDisabled&quot; disabled&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customCheckDisabled&quot;&gt;Check this custom checkbox&lt;/label&gt;
    &lt;/div&gt;

    &lt;div class=&quot;custom-control custom-radio&quot;&gt;
      &lt;input type=&quot;radio&quot; id=&quot;radio3&quot; name=&quot;radioDisabled&quot; id=&quot;customRadioDisabled&quot; class=&quot;custom-control-input&quot; disabled&gt;
      &lt;label class=&quot;custom-control-label&quot; for=&quot;customRadioDisabled&quot;&gt;Toggle this custom radio&lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <h3>Range</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <label for="customRange1">Example range</label>
            <input type="range" class="custom-range" id="customRange1">
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;label for=&quot;customRange1&quot;&gt;Example range&lt;/label&gt;
    &lt;input type=&quot;range&quot; class=&quot;custom-range&quot; id=&quot;customRange1&quot;&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Input groups</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="input-group mb-3">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="basic-addon1">@</span>
                </div>
                <input type="text" class="form-control" placeholder="Username" aria-label="Username" aria-describedby="basic-addon1">
            </div>

            <div class="input-group mb-3">
                <input type="text" class="form-control" placeholder="Recipient's username" aria-label="Recipient's username" aria-describedby="basic-addon2">
                <div class="input-group-append">
                    <span class="input-group-text" id="basic-addon2">@example.com</span>
                </div>
            </div>

            <label for="basic-url">Your vanity URL</label>
            <div class="input-group mb-3">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="basic-addon3">https://example.com/users/</span>
                </div>
                <input type="text" class="form-control" id="basic-url" aria-describedby="basic-addon3">
            </div>

            <div class="input-group mb-3">
                <div class="input-group-prepend">
                    <span class="input-group-text">$</span>
                </div>
                <input type="text" class="form-control" aria-label="Amount (to the nearest dollar)">
                <div class="input-group-append">
                    <span class="input-group-text">.00</span>
                </div>
            </div>

            <div class="input-group">
                <div class="input-group-prepend">
                    <span class="input-group-text">With textarea</span>
                </div>
                <textarea class="form-control" aria-label="With textarea"></textarea>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;input-group mb-3&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot; id=&quot;basic-addon1&quot;&gt;@&lt;/span&gt;
      &lt;/div&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; placeholder=&quot;Username&quot; aria-label=&quot;Username&quot; aria-describedby=&quot;basic-addon1&quot;&gt;
    &lt;/div&gt;

    &lt;div class=&quot;input-group mb-3&quot;&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; placeholder=&quot;Recipient's username&quot; aria-label=&quot;Recipient's username&quot; aria-describedby=&quot;basic-addon2&quot;&gt;
      &lt;div class=&quot;input-group-append&quot;&gt;
        &lt;span class=&quot;input-group-text&quot; id=&quot;basic-addon2&quot;&gt;@example.com&lt;/span&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;label for=&quot;basic-url&quot;&gt;Your vanity URL&lt;/label&gt;
    &lt;div class=&quot;input-group mb-3&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot; id=&quot;basic-addon3&quot;&gt;https://example.com/users/&lt;/span&gt;
      &lt;/div&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; id=&quot;basic-url&quot; aria-describedby=&quot;basic-addon3&quot;&gt;
    &lt;/div&gt;

    &lt;div class=&quot;input-group mb-3&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot;&gt;$&lt;/span&gt;
      &lt;/div&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; aria-label=&quot;Amount (to the nearest dollar)&quot;&gt;
      &lt;div class=&quot;input-group-append&quot;&gt;
        &lt;span class=&quot;input-group-text&quot;&gt;.00&lt;/span&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;div class=&quot;input-group&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot;&gt;With textarea&lt;/span&gt;
      &lt;/div&gt;
      &lt;textarea class=&quot;form-control&quot; aria-label=&quot;With textarea&quot;&gt;&lt;/textarea&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <h3>Sizing</h3>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="input-group input-group-sm mb-3">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="inputGroup-sizing-sm">Small</span>
                </div>
                <input type="text" class="form-control" aria-label="Sizing example input" aria-describedby="inputGroup-sizing-sm">
            </div>

            <div class="input-group mb-3">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="inputGroup-sizing-default">Default</span>
                </div>
                <input type="text" class="form-control" aria-label="Sizing example input" aria-describedby="inputGroup-sizing-default">
            </div>

            <div class="input-group input-group-lg">
                <div class="input-group-prepend">
                    <span class="input-group-text" id="inputGroup-sizing-lg">Large</span>
                </div>
                <input type="text" class="form-control" aria-label="Sizing example input" aria-describedby="inputGroup-sizing-lg">
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;input-group input-group-sm mb-3&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot; id=&quot;inputGroup-sizing-sm&quot;&gt;Small&lt;/span&gt;
      &lt;/div&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; aria-label=&quot;Sizing example input&quot; aria-describedby=&quot;inputGroup-sizing-sm&quot;&gt;
    &lt;/div&gt;

    &lt;div class=&quot;input-group mb-3&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot; id=&quot;inputGroup-sizing-default&quot;&gt;Default&lt;/span&gt;
      &lt;/div&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; aria-label=&quot;Sizing example input&quot; aria-describedby=&quot;inputGroup-sizing-default&quot;&gt;
    &lt;/div&gt;

    &lt;div class=&quot;input-group input-group-lg&quot;&gt;
      &lt;div class=&quot;input-group-prepend&quot;&gt;
        &lt;span class=&quot;input-group-text&quot; id=&quot;inputGroup-sizing-lg&quot;&gt;Large&lt;/span&gt;
      &lt;/div&gt;
      &lt;input type=&quot;text&quot; class=&quot;form-control&quot; aria-label=&quot;Sizing example input&quot; aria-describedby=&quot;inputGroup-sizing-lg&quot;&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<h1>Jumbotron</h1>

<section>
    <h2>Jumbotron</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="jumbotron">
                <h1 class="display-4">Hello, world!</h1>
                <p class="lead">This is a simple hero unit, a simple jumbotron-style component for calling extra attention to featured content or information.</p>
                <hr class="my-4">
                <p>It uses utility classes for typography and spacing to space content out within the larger container.</p>
                <a class="btn btn-primary btn-lg" href="#" role="button">Learn more</a>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;jumbotron&quot;&gt;
      &lt;h1 class=&quot;display-4&quot;&gt;Hello, world!&lt;/h1&gt;
      &lt;p class=&quot;lead&quot;&gt;This is a simple hero unit, a simple jumbotron-style component for calling extra attention to featured content or information.&lt;/p&gt;
      &lt;hr class=&quot;my-4&quot;&gt;
      &lt;p&gt;It uses utility classes for typography and spacing to space content out within the larger container.&lt;/p&gt;
      &lt;a class=&quot;btn btn-primary btn-lg&quot; href=&quot;#&quot; role=&quot;button&quot;&gt;Learn more&lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <div class="jumbotron jumbotron-fluid">
                <div class="container">
                    <h1 class="display-4">Fluid jumbotron</h1>
                    <p class="lead">This is a modified jumbotron that occupies the entire horizontal space of its parent.</p>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;jumbotron jumbotron-fluid&quot;&gt;
      &lt;div class=&quot;container&quot;&gt;
        &lt;h1 class=&quot;display-4&quot;&gt;Fluid jumbotron&lt;/h1&gt;
        &lt;p class=&quot;lead&quot;&gt;This is a modified jumbotron that occupies the entire horizontal space of its parent.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<h1>List group</h1>

<section>
    <h2>Basic example</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="list-group">
                <li class="list-group-item">Cras justo odio</li>
                <li class="list-group-item">Dapibus ac facilisis in</li>
                <li class="list-group-item">Morbi leo risus</li>
                <li class="list-group-item">Porta ac consectetur ac</li>
                <li class="list-group-item">Vestibulum at eros</li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-group&quot;&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Cras justo odio&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Morbi leo risus&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Porta ac consectetur ac&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Vestibulum at eros&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Active items</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="list-group">
                <li class="list-group-item active">Cras justo odio</li>
                <li class="list-group-item">Dapibus ac facilisis in</li>
                <li class="list-group-item">Morbi leo risus</li>
                <li class="list-group-item">Porta ac consectetur ac</li>
                <li class="list-group-item">Vestibulum at eros</li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-group&quot;&gt;
      &lt;li class=&quot;list-group-item active&quot;&gt;Cras justo odio&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Morbi leo risus&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Porta ac consectetur ac&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Vestibulum at eros&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Disabled items</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="list-group">
                <li class="list-group-item disabled">Cras justo odio</li>
                <li class="list-group-item">Dapibus ac facilisis in</li>
                <li class="list-group-item">Morbi leo risus</li>
                <li class="list-group-item">Porta ac consectetur ac</li>
                <li class="list-group-item">Vestibulum at eros</li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-group&quot;&gt;
      &lt;li class=&quot;list-group-item disabled&quot;&gt;Cras justo odio&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Morbi leo risus&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Porta ac consectetur ac&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Vestibulum at eros&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Links and buttons</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="list-group">
                <a href="#" class="list-group-item list-group-item-action active">
                    Cras justo odio
                </a>
                <a href="#" class="list-group-item list-group-item-action">Dapibus ac facilisis in</a>
                <a href="#" class="list-group-item list-group-item-action">Morbi leo risus</a>
                <a href="#" class="list-group-item list-group-item-action">Porta ac consectetur ac</a>
                <a href="#" class="list-group-item list-group-item-action disabled">Vestibulum at eros</a>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;list-group&quot;&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action active&quot;&gt;
        Cras justo odio
      &lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action&quot;&gt;Dapibus ac facilisis in&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action&quot;&gt;Morbi leo risus&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action&quot;&gt;Porta ac consectetur ac&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action disabled&quot;&gt;Vestibulum at eros&lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Flush</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="list-group list-group-flush">
                <li class="list-group-item">Cras justo odio</li>
                <li class="list-group-item">Dapibus ac facilisis in</li>
                <li class="list-group-item">Morbi leo risus</li>
                <li class="list-group-item">Porta ac consectetur ac</li>
                <li class="list-group-item">Vestibulum at eros</li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-group list-group-flush&quot;&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Cras justo odio&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Morbi leo risus&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Porta ac consectetur ac&lt;/li&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Vestibulum at eros&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Contextual classes</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="list-group">
                <li class="list-group-item">Dapibus ac facilisis in</li>


                <li class="list-group-item list-group-item-primary">A simple primary list group item</li>
                <li class="list-group-item list-group-item-secondary">A simple secondary list group item</li>
                <li class="list-group-item list-group-item-success">A simple success list group item</li>
                <li class="list-group-item list-group-item-danger">A simple danger list group item</li>
                <li class="list-group-item list-group-item-warning">A simple warning list group item</li>
                <li class="list-group-item list-group-item-info">A simple info list group item</li>
                <li class="list-group-item list-group-item-light">A simple light list group item</li>
                <li class="list-group-item list-group-item-dark">A simple dark list group item</li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-group&quot;&gt;
      &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;


      &lt;li class=&quot;list-group-item list-group-item-primary&quot;&gt;A simple primary list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-secondary&quot;&gt;A simple secondary list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-success&quot;&gt;A simple success list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-danger&quot;&gt;A simple danger list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-warning&quot;&gt;A simple warning list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-info&quot;&gt;A simple info list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-light&quot;&gt;A simple light list group item&lt;/li&gt;
      &lt;li class=&quot;list-group-item list-group-item-dark&quot;&gt;A simple dark list group item&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <div class="list-group">
                <a href="#" class="list-group-item list-group-item-action">Dapibus ac facilisis in</a>


                <a href="#" class="list-group-item list-group-item-action list-group-item-primary">A simple primary list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-secondary">A simple secondary list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-success">A simple success list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-danger">A simple danger list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-warning">A simple warning list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-info">A simple info list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-light">A simple light list group item</a>
                <a href="#" class="list-group-item list-group-item-action list-group-item-dark">A simple dark list group item</a>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;list-group&quot;&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action&quot;&gt;Dapibus ac facilisis in&lt;/a&gt;


      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-primary&quot;&gt;A simple primary list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-secondary&quot;&gt;A simple secondary list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-success&quot;&gt;A simple success list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-danger&quot;&gt;A simple danger list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-warning&quot;&gt;A simple warning list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-info&quot;&gt;A simple info list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-light&quot;&gt;A simple light list group item&lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action list-group-item-dark&quot;&gt;A simple dark list group item&lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>With badges</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="list-group">
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    Cras justo odio
                    <span class="badge badge-primary badge-pill">14</span>
                </li>
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    Dapibus ac facilisis in
                    <span class="badge badge-primary badge-pill">2</span>
                </li>
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    Morbi leo risus
                    <span class="badge badge-primary badge-pill">1</span>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-group&quot;&gt;
      &lt;li class=&quot;list-group-item d-flex justify-content-between align-items-center&quot;&gt;
        Cras justo odio
        &lt;span class=&quot;badge badge-primary badge-pill&quot;&gt;14&lt;/span&gt;
      &lt;/li&gt;
      &lt;li class=&quot;list-group-item d-flex justify-content-between align-items-center&quot;&gt;
        Dapibus ac facilisis in
        &lt;span class=&quot;badge badge-primary badge-pill&quot;&gt;2&lt;/span&gt;
      &lt;/li&gt;
      &lt;li class=&quot;list-group-item d-flex justify-content-between align-items-center&quot;&gt;
        Morbi leo risus
        &lt;span class=&quot;badge badge-primary badge-pill&quot;&gt;1&lt;/span&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Custom content</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="list-group">
                <a href="#" class="list-group-item list-group-item-action flex-column align-items-start active">
                    <div class="d-flex w-100 justify-content-between">
                        <h5 class="mb-1">List group item heading</h5>
                        <small>3 days ago</small>
                    </div>
                    <p class="mb-1">Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit.</p>
                    <small>Donec id elit non mi porta.</small>
                </a>
                <a href="#" class="list-group-item list-group-item-action flex-column align-items-start">
                    <div class="d-flex w-100 justify-content-between">
                        <h5 class="mb-1">List group item heading</h5>
                        <small class="text-muted">3 days ago</small>
                    </div>
                    <p class="mb-1">Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit.</p>
                    <small class="text-muted">Donec id elit non mi porta.</small>
                </a>
                <a href="#" class="list-group-item list-group-item-action flex-column align-items-start">
                    <div class="d-flex w-100 justify-content-between">
                        <h5 class="mb-1">List group item heading</h5>
                        <small class="text-muted">3 days ago</small>
                    </div>
                    <p class="mb-1">Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit.</p>
                    <small class="text-muted">Donec id elit non mi porta.</small>
                </a>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;list-group&quot;&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action flex-column align-items-start active&quot;&gt;
        &lt;div class=&quot;d-flex w-100 justify-content-between&quot;&gt;
          &lt;h5 class=&quot;mb-1&quot;&gt;List group item heading&lt;/h5&gt;
          &lt;small&gt;3 days ago&lt;/small&gt;
        &lt;/div&gt;
        &lt;p class=&quot;mb-1&quot;&gt;Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit.&lt;/p&gt;
        &lt;small&gt;Donec id elit non mi porta.&lt;/small&gt;
      &lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action flex-column align-items-start&quot;&gt;
        &lt;div class=&quot;d-flex w-100 justify-content-between&quot;&gt;
          &lt;h5 class=&quot;mb-1&quot;&gt;List group item heading&lt;/h5&gt;
          &lt;small class=&quot;text-muted&quot;&gt;3 days ago&lt;/small&gt;
        &lt;/div&gt;
        &lt;p class=&quot;mb-1&quot;&gt;Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit.&lt;/p&gt;
        &lt;small class=&quot;text-muted&quot;&gt;Donec id elit non mi porta.&lt;/small&gt;
      &lt;/a&gt;
      &lt;a href=&quot;#&quot; class=&quot;list-group-item list-group-item-action flex-column align-items-start&quot;&gt;
        &lt;div class=&quot;d-flex w-100 justify-content-between&quot;&gt;
          &lt;h5 class=&quot;mb-1&quot;&gt;List group item heading&lt;/h5&gt;
          &lt;small class=&quot;text-muted&quot;&gt;3 days ago&lt;/small&gt;
        &lt;/div&gt;
        &lt;p class=&quot;mb-1&quot;&gt;Donec id elit non mi porta gravida at eget metus. Maecenas sed diam eget risus varius blandit.&lt;/p&gt;
        &lt;small class=&quot;text-muted&quot;&gt;Donec id elit non mi porta.&lt;/small&gt;
      &lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Custom content</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="row">
                <div class="col-4">
                    <div class="list-group" id="list-tab" role="tablist">
                        <a class="list-group-item list-group-item-action active" id="list-home-list" data-toggle="list" href="#list-home" role="tab" aria-controls="home">Home</a>
                        <a class="list-group-item list-group-item-action" id="list-profile-list" data-toggle="list" href="#list-profile" role="tab" aria-controls="profile">Profile</a>
                        <a class="list-group-item list-group-item-action" id="list-messages-list" data-toggle="list" href="#list-messages" role="tab" aria-controls="messages">Messages</a>
                        <a class="list-group-item list-group-item-action" id="list-settings-list" data-toggle="list" href="#list-settings" role="tab" aria-controls="settings">Settings</a>
                    </div>
                </div>
                <div class="col-8">
                    <div class="tab-content" id="nav-tabContent">
                        <div class="tab-pane fade show active" id="list-home" role="tabpanel" aria-labelledby="list-home-list">Velit aute mollit ipsum ad dolor consectetur nulla officia culpa adipisicing exercitation fugiat tempor. Voluptate deserunt sit sunt nisi aliqua fugiat proident ea ut. Mollit voluptate reprehenderit occaecat nisi ad non minim tempor sunt voluptate consectetur exercitation id ut nulla. Ea et fugiat aliquip nostrud sunt incididunt consectetur culpa aliquip eiusmod dolor. Anim ad Lorem aliqua in cupidatat nisi enim eu nostrud do aliquip veniam minim.</div>
                        <div class="tab-pane fade" id="list-profile" role="tabpanel" aria-labelledby="list-profile-list">Cupidatat quis ad sint excepteur laborum in esse qui. Et excepteur consectetur ex nisi eu do cillum ad laborum. Mollit et eu officia dolore sunt Lorem culpa qui commodo velit ex amet id ex. Officia anim incididunt laboris deserunt anim aute dolor incididunt veniam aute dolore do exercitation. Dolor nisi culpa ex ad irure in elit eu dolore. Ad laboris ipsum reprehenderit irure non commodo enim culpa commodo veniam incididunt veniam ad.</div>
                        <div class="tab-pane fade" id="list-messages" role="tabpanel" aria-labelledby="list-messages-list">Ut ut do pariatur aliquip aliqua aliquip exercitation do nostrud commodo reprehenderit aute ipsum voluptate. Irure Lorem et laboris nostrud amet cupidatat cupidatat anim do ut velit mollit consequat enim tempor. Consectetur est minim nostrud nostrud consectetur irure labore voluptate irure. Ipsum id Lorem sit sint voluptate est pariatur eu ad cupidatat et deserunt culpa sit eiusmod deserunt. Consectetur et fugiat anim do eiusmod aliquip nulla laborum elit adipisicing pariatur cillum.</div>
                        <div class="tab-pane fade" id="list-settings" role="tabpanel" aria-labelledby="list-settings-list">Irure enim occaecat labore sit qui aliquip reprehenderit amet velit. Deserunt ullamco ex elit nostrud ut dolore nisi officia magna sit occaecat laboris sunt dolor. Nisi eu minim cillum occaecat aute est cupidatat aliqua labore aute occaecat ea aliquip sunt amet. Aute mollit dolor ut exercitation irure commodo non amet consectetur quis amet culpa. Quis ullamco nisi amet qui aute irure eu. Magna labore dolor quis ex labore id nostrud deserunt dolor eiusmod eu pariatur culpa mollit in irure.</div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;row&quot;&gt;
      &lt;div class=&quot;col-4&quot;&gt;
        &lt;div class=&quot;list-group&quot; id=&quot;list-tab&quot; role=&quot;tablist&quot;&gt;
          &lt;a class=&quot;list-group-item list-group-item-action active&quot; id=&quot;list-home-list&quot; data-toggle=&quot;list&quot; href=&quot;#list-home&quot; role=&quot;tab&quot; aria-controls=&quot;home&quot;&gt;Home&lt;/a&gt;
          &lt;a class=&quot;list-group-item list-group-item-action&quot; id=&quot;list-profile-list&quot; data-toggle=&quot;list&quot; href=&quot;#list-profile&quot; role=&quot;tab&quot; aria-controls=&quot;profile&quot;&gt;Profile&lt;/a&gt;
          &lt;a class=&quot;list-group-item list-group-item-action&quot; id=&quot;list-messages-list&quot; data-toggle=&quot;list&quot; href=&quot;#list-messages&quot; role=&quot;tab&quot; aria-controls=&quot;messages&quot;&gt;Messages&lt;/a&gt;
          &lt;a class=&quot;list-group-item list-group-item-action&quot; id=&quot;list-settings-list&quot; data-toggle=&quot;list&quot; href=&quot;#list-settings&quot; role=&quot;tab&quot; aria-controls=&quot;settings&quot;&gt;Settings&lt;/a&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;col-8&quot;&gt;
        &lt;div class=&quot;tab-content&quot; id=&quot;nav-tabContent&quot;&gt;
          &lt;div class=&quot;tab-pane fade show active&quot; id=&quot;list-home&quot; role=&quot;tabpanel&quot; aria-labelledby=&quot;list-home-list&quot;&gt;...&lt;/div&gt;
          &lt;div class=&quot;tab-pane fade&quot; id=&quot;list-profile&quot; role=&quot;tabpanel&quot; aria-labelledby=&quot;list-profile-list&quot;&gt;...&lt;/div&gt;
          &lt;div class=&quot;tab-pane fade&quot; id=&quot;list-messages&quot; role=&quot;tabpanel&quot; aria-labelledby=&quot;list-messages-list&quot;&gt;...&lt;/div&gt;
          &lt;div class=&quot;tab-pane fade&quot; id=&quot;list-settings&quot; role=&quot;tabpanel&quot; aria-labelledby=&quot;list-settings-list&quot;&gt;...&lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<h1>Modal</h1>

<section>
    <h2>Live demo</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#exampleModal">
                Launch demo modal
            </button>

            <div class="modal fade" id="exampleModal" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="exampleModalLabel">Modal title</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            Woohoo, you're reading this text in a modal!
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                            <button type="button" class="btn btn-primary">Save changes</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;!-- Button trigger modal --&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; data-toggle=&quot;modal&quot; data-target=&quot;#exampleModal&quot;&gt;
      Launch demo modal
    &lt;/button&gt;

    &lt;!-- Modal --&gt;
    &lt;div class=&quot;modal fade&quot; id=&quot;exampleModal&quot; tabindex=&quot;-1&quot; role=&quot;dialog&quot; aria-labelledby=&quot;exampleModalLabel&quot; aria-hidden=&quot;true&quot;&gt;
      &lt;div class=&quot;modal-dialog&quot; role=&quot;document&quot;&gt;
        &lt;div class=&quot;modal-content&quot;&gt;
          &lt;div class=&quot;modal-header&quot;&gt;
            &lt;h5 class=&quot;modal-title&quot; id=&quot;exampleModalLabel&quot;&gt;Modal title&lt;/h5&gt;
            &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot; aria-label=&quot;Close&quot;&gt;
              &lt;span aria-hidden=&quot;true&quot;&gt;&amp;times;&lt;/span&gt;
            &lt;/button&gt;
          &lt;/div&gt;
          &lt;div class=&quot;modal-body&quot;&gt;
            ...
          &lt;/div&gt;
          &lt;div class=&quot;modal-footer&quot;&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-dismiss=&quot;modal&quot;&gt;Close&lt;/button&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot;&gt;Save changes&lt;/button&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Scrolling long content</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <!-- Button trigger modal -->
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#exampleModalLong">
                Launch demo modal
            </button>

            <!-- Modal -->
            <div class="modal fade" id="exampleModalLong" tabindex="-1" role="dialog" aria-labelledby="exampleModalLongTitle" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="exampleModalLongTitle">Modal title</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <p>Cras mattis consectetur purus sit amet fermentum. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
                            <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.</p>
                            <p>Aenean lacinia bibendum nulla sed consectetur. Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Donec sed odio dui. Donec ullamcorper nulla non metus auctor fringilla.</p>
                            <p>Cras mattis consectetur purus sit amet fermentum. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
                            <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.</p>
                            <p>Aenean lacinia bibendum nulla sed consectetur. Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Donec sed odio dui. Donec ullamcorper nulla non metus auctor fringilla.</p>
                            <p>Cras mattis consectetur purus sit amet fermentum. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
                            <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.</p>
                            <p>Aenean lacinia bibendum nulla sed consectetur. Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Donec sed odio dui. Donec ullamcorper nulla non metus auctor fringilla.</p>
                            <p>Cras mattis consectetur purus sit amet fermentum. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
                            <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.</p>
                            <p>Aenean lacinia bibendum nulla sed consectetur. Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Donec sed odio dui. Donec ullamcorper nulla non metus auctor fringilla.</p>
                            <p>Cras mattis consectetur purus sit amet fermentum. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
                            <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.</p>
                            <p>Aenean lacinia bibendum nulla sed consectetur. Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Donec sed odio dui. Donec ullamcorper nulla non metus auctor fringilla.</p>
                            <p>Cras mattis consectetur purus sit amet fermentum. Cras justo odio, dapibus ac facilisis in, egestas eget quam. Morbi leo risus, porta ac consectetur ac, vestibulum at eros.</p>
                            <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor.</p>
                            <p>Aenean lacinia bibendum nulla sed consectetur. Praesent commodo cursus magna, vel scelerisque nisl consectetur et. Donec sed odio dui. Donec ullamcorper nulla non metus auctor fringilla.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                            <button type="button" class="btn btn-primary">Save changes</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;!-- Button trigger modal --&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; data-toggle=&quot;modal&quot; data-target=&quot;#exampleModalLong&quot;&gt;
      Launch demo modal
    &lt;/button&gt;

    &lt;!-- Modal --&gt;
    &lt;div class=&quot;modal fade&quot; id=&quot;exampleModalLong&quot; tabindex=&quot;-1&quot; role=&quot;dialog&quot; aria-labelledby=&quot;exampleModalLongTitle&quot; aria-hidden=&quot;true&quot;&gt;
      &lt;div class=&quot;modal-dialog&quot; role=&quot;document&quot;&gt;
        &lt;div class=&quot;modal-content&quot;&gt;
          &lt;div class=&quot;modal-header&quot;&gt;
            &lt;h5 class=&quot;modal-title&quot; id=&quot;exampleModalLongTitle&quot;&gt;Modal title&lt;/h5&gt;
            &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot; aria-label=&quot;Close&quot;&gt;
              &lt;span aria-hidden=&quot;true&quot;&gt;&amp;times;&lt;/span&gt;
            &lt;/button&gt;
          &lt;/div&gt;
          &lt;div class=&quot;modal-body&quot;&gt;
            ...
          &lt;/div&gt;
          &lt;div class=&quot;modal-footer&quot;&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-dismiss=&quot;modal&quot;&gt;Close&lt;/button&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot;&gt;Save changes&lt;/button&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Vertically centered</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <!-- Button trigger modal -->
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#exampleModalCenter">
                Launch demo modal
            </button>

            <!-- Modal -->
            <div class="modal fade" id="exampleModalCenter" tabindex="-1" role="dialog" aria-labelledby="exampleModalCenterTitle" aria-hidden="true">
                <div class="modal-dialog modal-dialog-centered" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="exampleModalCenterTitle">Modal title</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            ...
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                            <button type="button" class="btn btn-primary">Save changes</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;!-- Button trigger modal --&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; data-toggle=&quot;modal&quot; data-target=&quot;#exampleModalCenter&quot;&gt;
      Launch demo modal
    &lt;/button&gt;

    &lt;!-- Modal --&gt;
    &lt;div class=&quot;modal fade&quot; id=&quot;exampleModalCenter&quot; tabindex=&quot;-1&quot; role=&quot;dialog&quot; aria-labelledby=&quot;exampleModalCenterTitle&quot; aria-hidden=&quot;true&quot;&gt;
      &lt;div class=&quot;modal-dialog modal-dialog-centered&quot; role=&quot;document&quot;&gt;
        &lt;div class=&quot;modal-content&quot;&gt;
          &lt;div class=&quot;modal-header&quot;&gt;
            &lt;h5 class=&quot;modal-title&quot; id=&quot;exampleModalCenterTitle&quot;&gt;Modal title&lt;/h5&gt;
            &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot; aria-label=&quot;Close&quot;&gt;
              &lt;span aria-hidden=&quot;true&quot;&gt;&amp;times;&lt;/span&gt;
            &lt;/button&gt;
          &lt;/div&gt;
          &lt;div class=&quot;modal-body&quot;&gt;
            ...
          &lt;/div&gt;
          &lt;div class=&quot;modal-footer&quot;&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-dismiss=&quot;modal&quot;&gt;Close&lt;/button&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot;&gt;Save changes&lt;/button&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Tooltips and popovers</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target="#exampleModalTooptip">
                Launch demo modal
            </button>

            <div class="modal fade" id="exampleModalTooptip" tabindex="-1" role="dialog" aria-labelledby="exampleModalLabel" aria-hidden="true">
                <div class="modal-dialog" role="document">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title" id="exampleModalLabel">Modal title</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            <h5>Popover in a modal</h5>
                            <p>This <a href="#" role="button" class="btn btn-secondary popover-test" data-toggle="popover" title="Popover title" data-content="Popover body content is set in this attribute.">button</a> triggers a popover on click.</p>
                            <hr>
                            <h5>Tooltips in a modal</h5>
                            <p><a href="#" class="tooltip-test" data-toggle="tooltip" title="Tooltip">This link</a> and <a href="#" class="tooltip-test" data-toggle="tooltip" title="Tooltip">that link</a> have tooltips on hover.</p>
                        </div>
                        <div class="modal-footer">
                            <button type="button" class="btn btn-secondary" data-dismiss="modal">Close</button>
                            <button type="button" class="btn btn-primary">Save changes</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;modal-body&quot;&gt;
      &lt;h5&gt;Popover in a modal&lt;/h5&gt;
      &lt;p&gt;This &lt;a href=&quot;#&quot; role=&quot;button&quot; class=&quot;btn btn-secondary popover-test&quot; data-toggle=&quot;popover&quot; title=&quot;Popover title&quot; data-content=&quot;Popover body content is set in this attribute.&quot;&gt;button&lt;/a&gt; triggers a popover on click.&lt;/p&gt;
      &lt;hr&gt;
      &lt;h5&gt;Tooltips in a modal&lt;/h5&gt;
      &lt;p&gt;&lt;a href=&quot;#&quot; class=&quot;tooltip-test&quot; data-toggle=&quot;tooltip&quot; title=&quot;Tooltip&quot;&gt;This link&lt;/a&gt; and &lt;a href=&quot;#&quot; class=&quot;tooltip-test&quot; data-toggle=&quot;tooltip&quot; title=&quot;Tooltip&quot;&gt;that link&lt;/a&gt; have tooltips on hover.&lt;/p&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Optional sizes</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <!-- Large modal -->
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target=".bd-example-modal-lg">Large modal</button>

            <div class="modal fade bd-example-modal-lg" tabindex="-1" role="dialog" aria-labelledby="myLargeModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-lg">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title h4" id="myLargeModalLabel">Large modal</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            ...
                        </div>
                    </div>
                </div>
            </div>

            <!-- Small modal -->
            <button type="button" class="btn btn-primary" data-toggle="modal" data-target=".bd-example-modal-sm">Small modal</button>

            <div class="modal fade bd-example-modal-sm" tabindex="-1" role="dialog" aria-labelledby="mySmallModalLabel" aria-hidden="true">
                <div class="modal-dialog modal-sm">
                    <div class="modal-content">
                        <div class="modal-header">
                            <h5 class="modal-title h4" id="myLargeModalLabel">Small modal</h5>
                            <button type="button" class="close" data-dismiss="modal" aria-label="Close">
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </div>
                        <div class="modal-body">
                            ...
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;!-- Large modal --&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; data-toggle=&quot;modal&quot; data-target=&quot;.bd-example-modal-lg&quot;&gt;Large modal&lt;/button&gt;

    &lt;div class=&quot;modal fade bd-example-modal-lg&quot; tabindex=&quot;-1&quot; role=&quot;dialog&quot; aria-labelledby=&quot;myLargeModalLabel&quot; aria-hidden=&quot;true&quot;&gt;
      &lt;div class=&quot;modal-dialog modal-lg&quot;&gt;
        &lt;div class=&quot;modal-content&quot;&gt;
          ...
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;!-- Small modal --&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; data-toggle=&quot;modal&quot; data-target=&quot;.bd-example-modal-sm&quot;&gt;Small modal&lt;/button&gt;

    &lt;div class=&quot;modal fade bd-example-modal-sm&quot; tabindex=&quot;-1&quot; role=&quot;dialog&quot; aria-labelledby=&quot;mySmallModalLabel&quot; aria-hidden=&quot;true&quot;&gt;
      &lt;div class=&quot;modal-dialog modal-sm&quot;&gt;
        &lt;div class=&quot;modal-content&quot;&gt;
          ...
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<h1>Navs</h1>

<section>
    <h2>Base nav</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <nav class="nav">
                <a class="nav-link active" href="#">Active</a>
                <a class="nav-link" href="#">Link</a>
                <a class="nav-link" href="#">Link</a>
                <a class="nav-link disabled" href="#">Disabled</a>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav class=&quot;nav&quot;&gt;
      &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Horizontal alignment</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav justify-content-center">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav justify-content-center&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <ul class="nav justify-content-end">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav justify-content-end&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Vertical</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav flex-column">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav flex-column&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Tabs</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav nav-tabs">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav nav-tabs&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Pills</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav nav-pills">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav nav-pills&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Fill and justify</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav nav-pills nav-fill">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Longer nav link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav nav-pills nav-fill&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Longer nav link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Working with flex utilities</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav class="nav nav-pills flex-column flex-sm-row">
                <a class="flex-sm-fill text-sm-center nav-link active" href="#">Active</a>
                <a class="flex-sm-fill text-sm-center nav-link" href="#">Link</a>
                <a class="flex-sm-fill text-sm-center nav-link" href="#">Link</a>
                <a class="flex-sm-fill text-sm-center nav-link disabled" href="#">Disabled</a>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav class=&quot;nav nav-pills flex-column flex-sm-row&quot;&gt;
      &lt;a class=&quot;flex-sm-fill text-sm-center nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;a class=&quot;flex-sm-fill text-sm-center nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;a class=&quot;flex-sm-fill text-sm-center nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;a class=&quot;flex-sm-fill text-sm-center nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Tabs with dropdowns</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav nav-tabs">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" aria-expanded="false">Dropdown</a>
                    <div class="dropdown-menu">
                        <a class="dropdown-item" href="#">Action</a>
                        <a class="dropdown-item" href="#">Another action</a>
                        <a class="dropdown-item" href="#">Something else here</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="#">Separated link</a>
                    </div>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav nav-tabs&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item dropdown&quot;&gt;
        &lt;a class=&quot;nav-link dropdown-toggle&quot; data-toggle=&quot;dropdown&quot; href=&quot;#&quot; role=&quot;button&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;Dropdown&lt;/a&gt;
        &lt;div class=&quot;dropdown-menu&quot;&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
          &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
        &lt;/div&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Pills with dropdowns</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <ul class="nav nav-pills">
                <li class="nav-item">
                    <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" data-toggle="dropdown" href="#" role="button" aria-haspopup="true" aria-expanded="false">Dropdown</a>
                    <div class="dropdown-menu">
                        <a class="dropdown-item" href="#">Action</a>
                        <a class="dropdown-item" href="#">Another action</a>
                        <a class="dropdown-item" href="#">Something else here</a>
                        <div class="dropdown-divider"></div>
                        <a class="dropdown-item" href="#">Separated link</a>
                    </div>
                </li>
                <li class="nav-item">
                    <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                    <a class="nav-link disabled" href="#">Disabled</a>
                </li>
            </ul>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;nav nav-pills&quot;&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item dropdown&quot;&gt;
        &lt;a class=&quot;nav-link dropdown-toggle&quot; data-toggle=&quot;dropdown&quot; href=&quot;#&quot; role=&quot;button&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;Dropdown&lt;/a&gt;
        &lt;div class=&quot;dropdown-menu&quot;&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
          &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
          &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
        &lt;/div&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
      &lt;/li&gt;
      &lt;li class=&quot;nav-item&quot;&gt;
        &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
      &lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>
</section>

<h1>Navbar</h1>

<section>
    <h2>Supported content</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav class="navbar navbar-expand-lg navbar-light bg-light">
                <a class="navbar-brand" href="#">Navbar</a>
                <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarSupportedContent" aria-controls="navbarSupportedContent" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>

                <div class="collapse navbar-collapse" id="navbarSupportedContent">
                    <ul class="navbar-nav mr-auto">
                        <li class="nav-item active">
                            <a class="nav-link" href="#">Home <span class="sr-only">(current)</span></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="#">Link</a>
                        </li>
                        <li class="nav-item dropdown">
                            <a class="nav-link dropdown-toggle" href="#" id="navbarDropdown" role="button" data-toggle="dropdown" aria-haspopup="true" aria-expanded="false">
                                Dropdown
                            </a>
                            <div class="dropdown-menu" aria-labelledby="navbarDropdown">
                                <a class="dropdown-item" href="#">Action</a>
                                <a class="dropdown-item" href="#">Another action</a>
                                <div class="dropdown-divider"></div>
                                <a class="dropdown-item" href="#">Something else here</a>
                            </div>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link disabled" href="#">Disabled</a>
                        </li>
                    </ul>
                    <form class="form-inline my-2 my-lg-0">
                        <input class="form-control mr-sm-2" type="search" placeholder="Search" aria-label="Search">
                        <button class="btn btn-outline-success my-2 my-sm-0" type="submit">Search</button>
                    </form>
                </div>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav class=&quot;navbar navbar-expand-lg navbar-light bg-light&quot;&gt;
      &lt;a class=&quot;navbar-brand&quot; href=&quot;#&quot;&gt;Navbar&lt;/a&gt;
      &lt;button class=&quot;navbar-toggler&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#navbarSupportedContent&quot; aria-controls=&quot;navbarSupportedContent&quot; aria-expanded=&quot;false&quot; aria-label=&quot;Toggle navigation&quot;&gt;
        &lt;span class=&quot;navbar-toggler-icon&quot;&gt;&lt;/span&gt;
      &lt;/button&gt;

      &lt;div class=&quot;collapse navbar-collapse&quot; id=&quot;navbarSupportedContent&quot;&gt;
        &lt;ul class=&quot;navbar-nav mr-auto&quot;&gt;
          &lt;li class=&quot;nav-item active&quot;&gt;
            &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Home &lt;span class=&quot;sr-only&quot;&gt;(current)&lt;/span&gt;&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item dropdown&quot;&gt;
            &lt;a class=&quot;nav-link dropdown-toggle&quot; href=&quot;#&quot; id=&quot;navbarDropdown&quot; role=&quot;button&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
              Dropdown
            &lt;/a&gt;
            &lt;div class=&quot;dropdown-menu&quot; aria-labelledby=&quot;navbarDropdown&quot;&gt;
              &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
              &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
              &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
              &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;/div&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
          &lt;/li&gt;
        &lt;/ul&gt;
        &lt;form class=&quot;form-inline my-2 my-lg-0&quot;&gt;
          &lt;input class=&quot;form-control mr-sm-2&quot; type=&quot;search&quot; placeholder=&quot;Search&quot; aria-label=&quot;Search&quot;&gt;
          &lt;button class=&quot;btn btn-outline-success my-2 my-sm-0&quot; type=&quot;submit&quot;&gt;Search&lt;/button&gt;
        &lt;/form&gt;
      &lt;/div&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Text</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav class="navbar navbar-light bg-light">
        <span class="navbar-text">
          Navbar text with an inline element
        </span>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav class=&quot;navbar navbar-light bg-light&quot;&gt;
      &lt;span class=&quot;navbar-text&quot;&gt;
        Navbar text with an inline element
      &lt;/span&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Responsive behavior</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav class="navbar navbar-expand-lg navbar-light bg-light">
                <button class="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarTogglerDemo01" aria-controls="navbarTogglerDemo01" aria-expanded="false" aria-label="Toggle navigation">
                    <span class="navbar-toggler-icon"></span>
                </button>
                <div class="collapse navbar-collapse" id="navbarTogglerDemo01">
                    <a class="navbar-brand" href="#">Hidden brand</a>
                    <ul class="navbar-nav mr-auto mt-2 mt-lg-0">
                        <li class="nav-item active">
                            <a class="nav-link" href="#">Home <span class="sr-only">(current)</span></a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link" href="#">Link</a>
                        </li>
                        <li class="nav-item">
                            <a class="nav-link disabled" href="#">Disabled</a>
                        </li>
                    </ul>
                    <form class="form-inline my-2 my-lg-0">
                        <input class="form-control mr-sm-2" type="search" placeholder="Search" aria-label="Search">
                        <button class="btn btn-outline-success my-2 my-sm-0" type="submit">Search</button>
                    </form>
                </div>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav class=&quot;navbar navbar-expand-lg navbar-light bg-light&quot;&gt;
      &lt;button class=&quot;navbar-toggler&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#navbarTogglerDemo01&quot; aria-controls=&quot;navbarTogglerDemo01&quot; aria-expanded=&quot;false&quot; aria-label=&quot;Toggle navigation&quot;&gt;
        &lt;span class=&quot;navbar-toggler-icon&quot;&gt;&lt;/span&gt;
      &lt;/button&gt;
      &lt;div class=&quot;collapse navbar-collapse&quot; id=&quot;navbarTogglerDemo01&quot;&gt;
        &lt;a class=&quot;navbar-brand&quot; href=&quot;#&quot;&gt;Hidden brand&lt;/a&gt;
        &lt;ul class=&quot;navbar-nav mr-auto mt-2 mt-lg-0&quot;&gt;
          &lt;li class=&quot;nav-item active&quot;&gt;
            &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Home &lt;span class=&quot;sr-only&quot;&gt;(current)&lt;/span&gt;&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
          &lt;/li&gt;
        &lt;/ul&gt;
        &lt;form class=&quot;form-inline my-2 my-lg-0&quot;&gt;
          &lt;input class=&quot;form-control mr-sm-2&quot; type=&quot;search&quot; placeholder=&quot;Search&quot; aria-label=&quot;Search&quot;&gt;
          &lt;button class=&quot;btn btn-outline-success my-2 my-sm-0&quot; type=&quot;submit&quot;&gt;Search&lt;/button&gt;
        &lt;/form&gt;
      &lt;/div&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<h1>Pagination</h1>

<section>
    <h2>Working with icons</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav aria-label="Page navigation example">
                <ul class="pagination">
                    <li class="page-item">
                        <a class="page-link" href="#" aria-label="Previous">
                            <span aria-hidden="true">&laquo;</span>
                            <span class="sr-only">Previous</span>
                        </a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">1</a></li>
                    <li class="page-item"><a class="page-link" href="#">2</a></li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                    <li class="page-item">
                        <a class="page-link" href="#" aria-label="Next">
                            <span aria-hidden="true">&raquo;</span>
                            <span class="sr-only">Next</span>
                        </a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;Page navigation example&quot;&gt;
      &lt;ul class=&quot;pagination&quot;&gt;
        &lt;li class=&quot;page-item&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; aria-label=&quot;Previous&quot;&gt;
            &lt;span aria-hidden=&quot;true&quot;&gt;&amp;laquo;&lt;/span&gt;
            &lt;span class=&quot;sr-only&quot;&gt;Previous&lt;/span&gt;
          &lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;1&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; aria-label=&quot;Next&quot;&gt;
            &lt;span aria-hidden=&quot;true&quot;&gt;&amp;raquo;&lt;/span&gt;
            &lt;span class=&quot;sr-only&quot;&gt;Next&lt;/span&gt;
          &lt;/a&gt;
        &lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Disabled and active states</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav aria-label="...">
                <ul class="pagination">
                    <li class="page-item disabled">
                        <a class="page-link" href="#" tabindex="-1">Previous</a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">1</a></li>
                    <li class="page-item active">
                        <a class="page-link" href="#">2 <span class="sr-only">(current)</span></a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                    <li class="page-item">
                        <a class="page-link" href="#">Next</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;...&quot;&gt;
      &lt;ul class=&quot;pagination&quot;&gt;
        &lt;li class=&quot;page-item disabled&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; tabindex=&quot;-1&quot;&gt;Previous&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;1&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item active&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2 &lt;span class=&quot;sr-only&quot;&gt;(current)&lt;/span&gt;&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;Next&lt;/a&gt;
        &lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Sizing</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav aria-label="...">
                <ul class="pagination pagination-lg">
                    <li class="page-item disabled">
                        <a class="page-link" href="#" tabindex="-1">1</a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">2</a></li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;...&quot;&gt;
      &lt;ul class=&quot;pagination pagination-lg&quot;&gt;
        &lt;li class=&quot;page-item disabled&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; tabindex=&quot;-1&quot;&gt;1&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <nav aria-label="...">
                <ul class="pagination">
                    <li class="page-item disabled">
                        <a class="page-link" href="#" tabindex="-1">1</a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">2</a></li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;...&quot;&gt;
      &lt;ul class=&quot;pagination&quot;&gt;
        &lt;li class=&quot;page-item disabled&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; tabindex=&quot;-1&quot;&gt;1&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <nav aria-label="...">
                <ul class="pagination pagination-sm">
                    <li class="page-item disabled">
                        <a class="page-link" href="#" tabindex="-1">1</a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">2</a></li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;...&quot;&gt;
      &lt;ul class=&quot;pagination pagination-sm&quot;&gt;
        &lt;li class=&quot;page-item disabled&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; tabindex=&quot;-1&quot;&gt;1&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Alignment</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <nav aria-label="Page navigation example">
                <ul class="pagination justify-content-center">
                    <li class="page-item disabled">
                        <a class="page-link" href="#" tabindex="-1">Previous</a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">1</a></li>
                    <li class="page-item"><a class="page-link" href="#">2</a></li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                    <li class="page-item">
                        <a class="page-link" href="#">Next</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;Page navigation example&quot;&gt;
      &lt;ul class=&quot;pagination justify-content-center&quot;&gt;
        &lt;li class=&quot;page-item disabled&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; tabindex=&quot;-1&quot;&gt;Previous&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;1&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;Next&lt;/a&gt;
        &lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>

    <div class="row">
        <div class="col-12">
            <nav aria-label="Page navigation example">
                <ul class="pagination justify-content-end">
                    <li class="page-item disabled">
                        <a class="page-link" href="#" tabindex="-1">Previous</a>
                    </li>
                    <li class="page-item"><a class="page-link" href="#">1</a></li>
                    <li class="page-item"><a class="page-link" href="#">2</a></li>
                    <li class="page-item"><a class="page-link" href="#">3</a></li>
                    <li class="page-item">
                        <a class="page-link" href="#">Next</a>
                    </li>
                </ul>
            </nav>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;Page navigation example&quot;&gt;
      &lt;ul class=&quot;pagination justify-content-end&quot;&gt;
        &lt;li class=&quot;page-item disabled&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot; tabindex=&quot;-1&quot;&gt;Previous&lt;/a&gt;
        &lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;1&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;2&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;&lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;3&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;page-item&quot;&gt;
          &lt;a class=&quot;page-link&quot; href=&quot;#&quot;&gt;Next&lt;/a&gt;
        &lt;/li&gt;
      &lt;/ul&gt;
    &lt;/nav&gt;
    </code>
  </pre>
</section>

<h1>Popovers</h1>

<section>
    <h2>Four directions</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <button type="button" class="btn btn-secondary" data-container="body" data-toggle="popover" data-placement="top" data-content="Vivamus sagittis lacus vel augue laoreet rutrum faucibus.">
                Popover on top
            </button>

            <button type="button" class="btn btn-secondary" data-container="body" data-toggle="popover" data-placement="right" data-content="Vivamus sagittis lacus vel augue laoreet rutrum faucibus.">
                Popover on right
            </button>

            <button type="button" class="btn btn-secondary" data-container="body" data-toggle="popover" data-placement="bottom" data-content="Vivamus
      sagittis lacus vel augue laoreet rutrum faucibus.">
                Popover on bottom
            </button>

            <button type="button" class="btn btn-secondary" data-container="body" data-toggle="popover" data-placement="left" data-content="Vivamus sagittis lacus vel augue laoreet rutrum faucibus.">
                Popover on left
            </button>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-container=&quot;body&quot; data-toggle=&quot;popover&quot; data-placement=&quot;top&quot; data-content=&quot;Vivamus sagittis lacus vel augue laoreet rutrum faucibus.&quot;&gt;
      Popover on top
    &lt;/button&gt;

    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-container=&quot;body&quot; data-toggle=&quot;popover&quot; data-placement=&quot;right&quot; data-content=&quot;Vivamus sagittis lacus vel augue laoreet rutrum faucibus.&quot;&gt;
      Popover on right
    &lt;/button&gt;

    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-container=&quot;body&quot; data-toggle=&quot;popover&quot; data-placement=&quot;bottom&quot; data-content=&quot;Vivamus sagittis lacus vel augue laoreet rutrum faucibus.&quot;&gt;
      Popover on bottom
    &lt;/button&gt;

    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-container=&quot;body&quot; data-toggle=&quot;popover&quot; data-placement=&quot;left&quot; data-content=&quot;Vivamus sagittis lacus vel augue laoreet rutrum faucibus.&quot;&gt;
      Popover on left
    &lt;/button&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Dismiss on next click</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <a tabindex="0" class="btn btn-lg btn-danger" role="button" data-toggle="popover" data-trigger="focus" title="Dismissible popover" data-content="And here's some amazing content. It's very engaging. Right?">Dismissible popover</a>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;a tabindex=&quot;0&quot; class=&quot;btn btn-lg btn-danger&quot; role=&quot;button&quot; data-toggle=&quot;popover&quot; data-trigger=&quot;focus&quot; title=&quot;Dismissible popover&quot; data-content=&quot;And here's some amazing content. It's very engaging. Right?&quot;&gt;Dismissible popover&lt;/a&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Disabled elements</h2>
    <hr>
    <div class="row">
        <div class="col-12">
      <span class="d-inline-block" data-toggle="popover" data-content="Disabled popover">
        <button class="btn btn-primary" style="pointer-events: none;" type="button" disabled>Disabled button</button>
      </span>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;span class=&quot;d-inline-block&quot; data-toggle=&quot;popover&quot; data-content=&quot;Disabled popover&quot;&gt;
      &lt;button class=&quot;btn btn-primary&quot; style=&quot;pointer-events: none;&quot; type=&quot;button&quot; disabled&gt;Disabled button&lt;/button&gt;
    &lt;/span&gt;
    </code>
  </pre>
</section>

<h1>Progress</h1>

<section>
    <h2>Labels</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="progress">
                <div class="progress-bar" role="progressbar" style="width: 25%;" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100">25%</div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar&quot; role=&quot;progressbar&quot; style=&quot;width: 25%;&quot; aria-valuenow=&quot;25&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;25%&lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Height</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="progress" style="height: 1px;">
                <div class="progress-bar" role="progressbar" style="width: 25%;" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress" style="height: 20px;">
                <div class="progress-bar" role="progressbar" style="width: 25%;" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;progress&quot; style=&quot;height: 1px;&quot;&gt;
      &lt;div class=&quot;progress-bar&quot; role=&quot;progressbar&quot; style=&quot;width: 25%;&quot; aria-valuenow=&quot;25&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot; style=&quot;height: 20px;&quot;&gt;
      &lt;div class=&quot;progress-bar&quot; role=&quot;progressbar&quot; style=&quot;width: 25%;&quot; aria-valuenow=&quot;25&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Backgrounds</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="progress mb-2">
                <div class="progress-bar bg-success" role="progressbar" style="width: 25%" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar bg-info" role="progressbar" style="width: 50%" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar bg-warning" role="progressbar" style="width: 75%" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar bg-danger" role="progressbar" style="width: 100%" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar bg-success&quot; role=&quot;progressbar&quot; style=&quot;width: 25%&quot; aria-valuenow=&quot;25&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar bg-info&quot; role=&quot;progressbar&quot; style=&quot;width: 50%&quot; aria-valuenow=&quot;50&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar bg-warning&quot; role=&quot;progressbar&quot; style=&quot;width: 75%&quot; aria-valuenow=&quot;75&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar bg-danger&quot; role=&quot;progressbar&quot; style=&quot;width: 100%&quot; aria-valuenow=&quot;100&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Multiple bars</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="progress">
                <div class="progress-bar" role="progressbar" style="width: 15%" aria-valuenow="15" aria-valuemin="0" aria-valuemax="100"></div>
                <div class="progress-bar bg-success" role="progressbar" style="width: 30%" aria-valuenow="30" aria-valuemin="0" aria-valuemax="100"></div>
                <div class="progress-bar bg-info" role="progressbar" style="width: 20%" aria-valuenow="20" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar&quot; role=&quot;progressbar&quot; style=&quot;width: 15%&quot; aria-valuenow=&quot;15&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
      &lt;div class=&quot;progress-bar bg-success&quot; role=&quot;progressbar&quot; style=&quot;width: 30%&quot; aria-valuenow=&quot;30&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
      &lt;div class=&quot;progress-bar bg-info&quot; role=&quot;progressbar&quot; style=&quot;width: 20%&quot; aria-valuenow=&quot;20&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Striped</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="progress mb-2">
                <div class="progress-bar progress-bar-striped" role="progressbar" style="width: 10%" aria-valuenow="10" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar progress-bar-striped bg-success" role="progressbar" style="width: 25%" aria-valuenow="25" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar progress-bar-striped bg-info" role="progressbar" style="width: 50%" aria-valuenow="50" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar progress-bar-striped bg-warning" role="progressbar" style="width: 75%" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
            <div class="progress mb-2">
                <div class="progress-bar progress-bar-striped bg-danger" role="progressbar" style="width: 100%" aria-valuenow="100" aria-valuemin="0" aria-valuemax="100"></div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar progress-bar-striped&quot; role=&quot;progressbar&quot; style=&quot;width: 10%&quot; aria-valuenow=&quot;10&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar progress-bar-striped bg-success&quot; role=&quot;progressbar&quot; style=&quot;width: 25%&quot; aria-valuenow=&quot;25&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar progress-bar-striped bg-info&quot; role=&quot;progressbar&quot; style=&quot;width: 50%&quot; aria-valuenow=&quot;50&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar progress-bar-striped bg-warning&quot; role=&quot;progressbar&quot; style=&quot;width: 75%&quot; aria-valuenow=&quot;75&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar progress-bar-striped bg-danger&quot; role=&quot;progressbar&quot; style=&quot;width: 100%&quot; aria-valuenow=&quot;100&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
    <h2>Animated stripes</h2>
    <hr>
    <div class="row">
        <div class="col-12">
            <div class="progress">
                <div class="progress-bar progress-bar-striped progress-bar-animated" role="progressbar" aria-valuenow="75" aria-valuemin="0" aria-valuemax="100" style="width: 75%"></div>
            </div>
        </div>
    </div>
    <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;progress&quot;&gt;
      &lt;div class=&quot;progress-bar progress-bar-striped progress-bar-animated&quot; role=&quot;progressbar&quot; aria-valuenow=&quot;75&quot; aria-valuemin=&quot;0&quot; aria-valuemax=&quot;100&quot; style=&quot;width: 75%&quot;&gt;&lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<h1>Tooltips</h1>

<section>
  <h2>Examples</h2>
  <hr>
  <div class="row">
    <div class="col-12">
      <button type="button" class="btn btn-secondary mr-2" data-toggle="tooltip" data-placement="top" title="Tooltip on top">
        Tooltip on top
      </button>
      <button type="button" class="btn btn-secondary mr-2" data-toggle="tooltip" data-placement="right" title="Tooltip on right">
        Tooltip on right
      </button>
      <button type="button" class="btn btn-secondary mr-2" data-toggle="tooltip" data-placement="bottom" title="Tooltip on bottom">
        Tooltip on bottom
      </button>
      <button type="button" class="btn btn-secondary mr-2" data-toggle="tooltip" data-placement="left" title="Tooltip on left">
        Tooltip on left
      </button>
      <button type="button" class="btn btn-secondary mr-2" data-toggle="tooltip" data-html="true" title="<em>Tooltip</em> <u>with</u> <b>HTML</b>">
        Tooltip with HTML
      </button>
    </div>
  </div>
  <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-toggle=&quot;tooltip&quot; data-placement=&quot;top&quot; title=&quot;Tooltip on top&quot;&gt;
      Tooltip on top
    &lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-toggle=&quot;tooltip&quot; data-placement=&quot;right&quot; title=&quot;Tooltip on right&quot;&gt;
      Tooltip on right
    &lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-toggle=&quot;tooltip&quot; data-placement=&quot;bottom&quot; title=&quot;Tooltip on bottom&quot;&gt;
      Tooltip on bottom
    &lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-toggle=&quot;tooltip&quot; data-placement=&quot;left&quot; title=&quot;Tooltip on left&quot;&gt;
      Tooltip on left
    &lt;/button&gt;

    &lt;!-- With Custom HTML --&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; data-toggle=&quot;tooltip&quot; data-html=&quot;true&quot; title=&quot;&lt;em&gt;Tooltip&lt;/em&gt; &lt;u&gt;with&lt;/u&gt; &lt;b&gt;HTML&lt;/b&gt;&quot;&gt;
      Tooltip with HTML
    &lt;/button&gt;
    </code>
  </pre>
</section>

<h1>Carousel</h1>

<section>
  <h2>Slides only</h2>
  <hr>
  <div class="row">
    <div class="col-12">
      <div id="carouselExampleSlidesOnly" class="carousel slide" data-ride="carousel">
        <div class="carousel-inner">
          <div class="carousel-item active">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-1.svg" alt="First slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-2.svg" alt="Second slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-3.svg" alt="Third slide">
          </div>
        </div>
      </div>
    </div>
  </div>
  <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div id=&quot;carouselExampleSlidesOnly&quot; class=&quot;carousel slide&quot; data-ride=&quot;carousel&quot;&gt;
      &lt;div class=&quot;carousel-inner&quot;&gt;
        &lt;div class=&quot;carousel-item active&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=777&amp;fg=555&amp;text=First slide&quot; alt=&quot;First slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=666&amp;fg=444&amp;text=Second slide&quot; alt=&quot;Second slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=555&amp;fg=333&amp;text=Third slide&quot; alt=&quot;Third slide&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
  <h2>With controls</h2>
  <hr>
  <div class="row">
    <div class="col-12">
      <div id="carouselExampleControls" class="carousel slide" data-ride="carousel">
        <div class="carousel-inner">
          <div class="carousel-item active">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-1.svg" alt="First slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-2.svg" alt="Second slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-3.svg" alt="Third slide">
          </div>
        </div>
        <a class="carousel-control-prev" href="#carouselExampleControls" role="button" data-slide="prev">
          <span class="carousel-control-prev-icon" aria-hidden="true"></span>
          <span class="sr-only">Previous</span>
        </a>
        <a class="carousel-control-next" href="#carouselExampleControls" role="button" data-slide="next">
          <span class="carousel-control-next-icon" aria-hidden="true"></span>
          <span class="sr-only">Next</span>
        </a>
      </div>
    </div>
  </div>
  <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div id=&quot;carouselExampleControls&quot; class=&quot;carousel slide&quot; data-ride=&quot;carousel&quot;&gt;
      &lt;div class=&quot;carousel-inner&quot;&gt;
        &lt;div class=&quot;carousel-item active&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=777&amp;fg=555&amp;text=First slide&quot; alt=&quot;First slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=666&amp;fg=444&amp;text=Second slide&quot; alt=&quot;Second slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=555&amp;fg=333&amp;text=Third slide&quot; alt=&quot;Third slide&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;a class=&quot;carousel-control-prev&quot; href=&quot;#carouselExampleControls&quot; role=&quot;button&quot; data-slide=&quot;prev&quot;&gt;
        &lt;span class=&quot;carousel-control-prev-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Previous&lt;/span&gt;
      &lt;/a&gt;
      &lt;a class=&quot;carousel-control-next&quot; href=&quot;#carouselExampleControls&quot; role=&quot;button&quot; data-slide=&quot;next&quot;&gt;
        &lt;span class=&quot;carousel-control-next-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Next&lt;/span&gt;
      &lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
  <h2>With indicators</h2>
  <hr>
  <div class="row">
    <div class="col-12">
      <div id="carouselExampleIndicators" class="carousel slide" data-ride="carousel">
        <ol class="carousel-indicators">
          <li data-target="#carouselExampleIndicators" data-slide-to="0" class="active"></li>
          <li data-target="#carouselExampleIndicators" data-slide-to="1"></li>
          <li data-target="#carouselExampleIndicators" data-slide-to="2"></li>
        </ol>
        <div class="carousel-inner">
          <div class="carousel-item active">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-1.svg" alt="First slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-2.svg" alt="Second slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-3.svg" alt="Third slide">
          </div>
        </div>
        <a class="carousel-control-prev" href="#carouselExampleIndicators" role="button" data-slide="prev">
          <span class="carousel-control-prev-icon" aria-hidden="true"></span>
          <span class="sr-only">Previous</span>
        </a>
        <a class="carousel-control-next" href="#carouselExampleIndicators" role="button" data-slide="next">
          <span class="carousel-control-next-icon" aria-hidden="true"></span>
          <span class="sr-only">Next</span>
        </a>
      </div>
    </div>
  </div>
  <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div id=&quot;carouselExampleIndicators&quot; class=&quot;carousel slide&quot; data-ride=&quot;carousel&quot;&gt;
      &lt;ol class=&quot;carousel-indicators&quot;&gt;
        &lt;li data-target=&quot;#carouselExampleIndicators&quot; data-slide-to=&quot;0&quot; class=&quot;active&quot;&gt;&lt;/li&gt;
        &lt;li data-target=&quot;#carouselExampleIndicators&quot; data-slide-to=&quot;1&quot;&gt;&lt;/li&gt;
        &lt;li data-target=&quot;#carouselExampleIndicators&quot; data-slide-to=&quot;2&quot;&gt;&lt;/li&gt;
      &lt;/ol&gt;
      &lt;div class=&quot;carousel-inner&quot;&gt;
        &lt;div class=&quot;carousel-item active&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=777&amp;fg=555&amp;text=First slide&quot; alt=&quot;First slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=666&amp;fg=444&amp;text=Second slide&quot; alt=&quot;Second slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=555&amp;fg=333&amp;text=Third slide&quot; alt=&quot;Third slide&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;a class=&quot;carousel-control-prev&quot; href=&quot;#carouselExampleIndicators&quot; role=&quot;button&quot; data-slide=&quot;prev&quot;&gt;
        &lt;span class=&quot;carousel-control-prev-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Previous&lt;/span&gt;
      &lt;/a&gt;
      &lt;a class=&quot;carousel-control-next&quot; href=&quot;#carouselExampleIndicators&quot; role=&quot;button&quot; data-slide=&quot;next&quot;&gt;
        &lt;span class=&quot;carousel-control-next-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Next&lt;/span&gt;
      &lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
  <h2>With captions</h2>
  <hr>
  <div class="row">
    <div class="col-12">
      <div id="carouselExampleCaptions" class="carousel slide" data-ride="carousel">
        <ol class="carousel-indicators">
          <li data-target="#carouselExampleCaptions" data-slide-to="0" class="active"></li>
          <li data-target="#carouselExampleCaptions" data-slide-to="1"></li>
          <li data-target="#carouselExampleCaptions" data-slide-to="2"></li>
        </ol>
        <div class="carousel-inner">
          <div class="carousel-item active">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-1.svg" alt="First slide">
            <div class="carousel-caption d-none d-md-block">
              <h5>First slide label</h5>
              <p>Nulla vitae elit libero, a pharetra augue mollis interdum.</p>
            </div>
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-2.svg" alt="Second slide">
            <div class="carousel-caption d-none d-md-block">
                <h5>Second slide label</h5>
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>
              </div>
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-3.svg" alt="Third slide">
            <div class="carousel-caption d-none d-md-block">
                <h5>Third slide label</h5>
                <p>Praesent commodo cursus magna, vel scelerisque nisl consectetur.</p>
              </div>
          </div>
        </div>
        <a class="carousel-control-prev" href="#carouselExampleCaptions" role="button" data-slide="prev">
          <span class="carousel-control-prev-icon" aria-hidden="true"></span>
          <span class="sr-only">Previous</span>
        </a>
        <a class="carousel-control-next" href="#carouselExampleCaptions" role="button" data-slide="next">
          <span class="carousel-control-next-icon" aria-hidden="true"></span>
          <span class="sr-only">Next</span>
        </a>
      </div>
    </div>
  </div>
  <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div id=&quot;carouselExampleCaptions&quot; class=&quot;carousel slide&quot; data-ride=&quot;carousel&quot;&gt;
      &lt;ol class=&quot;carousel-indicators&quot;&gt;
        &lt;li data-target=&quot;#carouselExampleCaptions&quot; data-slide-to=&quot;0&quot; class=&quot;active&quot;&gt;&lt;/li&gt;
        &lt;li data-target=&quot;#carouselExampleCaptions&quot; data-slide-to=&quot;1&quot;&gt;&lt;/li&gt;
        &lt;li data-target=&quot;#carouselExampleCaptions&quot; data-slide-to=&quot;2&quot;&gt;&lt;/li&gt;
      &lt;/ol&gt;
      &lt;div class=&quot;carousel-inner&quot;&gt;
        &lt;div class=&quot;carousel-item active&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-1.svg&quot; alt=&quot;First slide&quot;&gt;
          &lt;div class=&quot;carousel-caption d-none d-md-block&quot;&gt;
            &lt;h5&gt;First slide label&lt;/h5&gt;
            &lt;p&gt;Nulla vitae elit libero, a pharetra augue mollis interdum.&lt;/p&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-2.svg&quot; alt=&quot;Second slide&quot;&gt;
          &lt;div class=&quot;carousel-caption d-none d-md-block&quot;&gt;
              &lt;h5&gt;Second slide label&lt;/h5&gt;
              &lt;p&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit.&lt;/p&gt;
            &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-3.svg&quot; alt=&quot;Third slide&quot;&gt;
          &lt;div class=&quot;carousel-caption d-none d-md-block&quot;&gt;
              &lt;h5&gt;Third slide label&lt;/h5&gt;
              &lt;p&gt;Praesent commodo cursus magna, vel scelerisque nisl consectetur.&lt;/p&gt;
            &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;a class=&quot;carousel-control-prev&quot; href=&quot;#carouselExampleCaptions&quot; role=&quot;button&quot; data-slide=&quot;prev&quot;&gt;
        &lt;span class=&quot;carousel-control-prev-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Previous&lt;/span&gt;
      &lt;/a&gt;
      &lt;a class=&quot;carousel-control-next&quot; href=&quot;#carouselExampleCaptions&quot; role=&quot;button&quot; data-slide=&quot;next&quot;&gt;
        &lt;span class=&quot;carousel-control-next-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Next&lt;/span&gt;
      &lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>

<section>
  <h2>Crossfade</h2>
  <hr>
  <div class="row">
    <div class="col-12">
      <div id="carouselExampleCrossfade" class="carousel slide carousel-fade" data-ride="carousel">
        <div class="carousel-inner">
          <div class="carousel-item active">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-1.svg" alt="First slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-2.svg" alt="Second slide">
          </div>
          <div class="carousel-item">
            <img class="d-block w-100" src="/templates/webjet8/ninja-starter-kit/assets/images/carousel-example/slide-3.svg" alt="Third slide">
          </div>
        </div>
        <a class="carousel-control-prev" href="#carouselExampleCrossfade" role="button" data-slide="prev">
          <span class="carousel-control-prev-icon" aria-hidden="true"></span>
          <span class="sr-only">Previous</span>
        </a>
        <a class="carousel-control-next" href="#carouselExampleCrossfade" role="button" data-slide="next">
          <span class="carousel-control-next-icon" aria-hidden="true"></span>
          <span class="sr-only">Next</span>
        </a>
      </div>
    </div>
  </div>
  <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div id=&quot;carouselExampleCrossfade&quot; class=&quot;carousel slide carousel-fade&quot; data-ride=&quot;carousel&quot;&gt;
      &lt;div class=&quot;carousel-inner&quot;&gt;
        &lt;div class=&quot;carousel-item active&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=777&amp;fg=555&amp;text=First slide&quot; alt=&quot;First slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=666&amp;fg=444&amp;text=Second slide&quot; alt=&quot;Second slide&quot;&gt;
        &lt;/div&gt;
        &lt;div class=&quot;carousel-item&quot;&gt;
          &lt;img class=&quot;d-block w-100&quot; src=&quot;.../800x400?auto=yes&amp;bg=555&amp;fg=333&amp;text=Third slide&quot; alt=&quot;Third slide&quot;&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;a class=&quot;carousel-control-prev&quot; href=&quot;#carouselExampleCrossfade&quot; role=&quot;button&quot; data-slide=&quot;prev&quot;&gt;
        &lt;span class=&quot;carousel-control-prev-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Previous&lt;/span&gt;
      &lt;/a&gt;
      &lt;a class=&quot;carousel-control-next&quot; href=&quot;#carouselExampleCrossfade&quot; role=&quot;button&quot; data-slide=&quot;next&quot;&gt;
        &lt;span class=&quot;carousel-control-next-icon&quot; aria-hidden=&quot;true&quot;&gt;&lt;/span&gt;
        &lt;span class=&quot;sr-only&quot;&gt;Next&lt;/span&gt;
      &lt;/a&gt;
    &lt;/div&gt;
    </code>
  </pre>
</section>


</div>
</div>
</div>