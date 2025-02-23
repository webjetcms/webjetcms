<%@ page pageEncoding="windows-1250" %>

<link rel="stylesheet" href="/templates/webjet8/ninja-starter-kit/assets/css/highlight.css">
<script src="/templates/webjet8/ninja-starter-kit/assets/js/plugins/highlight.js"></script>

<link rel="stylesheet" href="/templates/webjet8/ninja-starter-kit/assets/css/fontawesome.min.css">
<script>hljs.initHighlightingOnLoad();</script>

<style>
  body{
    padding: 0;
    margin: 0;
  }
  .structure-list ul{
    overflow-y: auto;
    max-height: 100%;
    width: 100%;
  }
  .structure-list ul li, .structure-list ul li ul{
    overflow-y: hidden;
  }
  .structure-list ul li a{
    color: #292929;
    text-decoration: none;
    font-size: 1.25rem;
  }
  .structure-list ul li a.anchor{
    color: #2b2b2b;
    text-decoration: none;
    font-size: 1rem;
  }
  .structure-list ul li a:hover{
    color: #0084ff;
  }
  .structure-list ul li a.anchor:hover:after{
    display: inline;
  }
  .structure-list ul li a.anchor:after{
    display: none;
    content: "  #";
    color: #0084ff;
  }
  .ly-article{
    border: none;
    padding: 0;
    margin: 0;
  }
  .override-style{
    color: rgb(0, 142, 207) !important;
    text-decoration: none !important;
    font-family: Arial, Helvetica, sans-serif !important;
    padding-top: 1.5rem !important;
  }
  .override-style a{
    display: none;
    text-decoration: none;
  }
  .override-style:hover a{
    display: inline;
  }
  h1.override-style{
    font-size: 3.5rem !important;
    font-weight: 300 !important;
  }

  h2.override-style{
    font-size: 2.5rem !important;
    font-weight: 300 !important;
    line-height: 1.2em !important;
  }

  h3.override-style{
    font-size: 1.5rem !important;
    font-weight: 300 !important;
    line-height: 1.2em !important;
  }
</style>

<script>
  $(function () {
    $("pre").not(".no-highlight").each(function (index) {
      $(this).find("code").attr("id", "collapse-" + index);
      $(this).before("<a class='btn btn-link btn-toggle float-right' data-toggle='custom-collapse' href='#collapse-" + index + "'>Show Code <i class='fa fa-angle-down'></i></a>");
    });

    $("[data-toggle='custom-collapse']").click(function (e) {
      e.preventDefault();
      toggleCollapse($(this).attr("href"), $(this));
    });

    $('[data-toggle="tooltip"]').tooltip();
    $('[data-toggle="popover"]').popover();

    generateStructure();

    $(".structure-list ul li a").click(function (e) {
      e.preventDefault();
      if ($(this).attr("data-toggle") == "collapse") return;
      var target = $(this).attr("href");

      $("html, body").animate({
        scrollTop: $(target).offset().top,
      }, 600);

      window.location.href = target;
    });

    $( '.structure-list ul' ).on( 'mousewheel DOMMouseScroll', function ( e ) {
      var e0 = e.originalEvent,
      delta = e0.wheelDelta || -e0.detail;

      this.scrollTop += ( delta < 0 ? 1 : -1 ) * 30;
      e.preventDefault();
    });
    
    var sectionToScroll = window.location.href.split("#")[1];
    
    setTimeout(function(){
      if(sectionToScroll != null){
        $("html, body").animate({
          scrollTop: $("#"+sectionToScroll).offset().top,
        }, 600);
      }
    }, 50);

  });

  function toggleCollapse(target, toggler) {

    if (target == "#show-all") {
      $("code.collapse").collapse('show');
      $("[data-toggle='custom-collapse']").not("[href='#show-all'], [href='#hide-all']").html("Hide Code <i class='fa fa-angle-up'></i>");
    } else if (target == "#hide-all") {
      $("code.collapse").collapse('hide');
      $("[data-toggle='custom-collapse']").not("[href='#show-all'], [href='#hide-all']").html("Show Code <i class='fa fa-angle-down'></i>");
    } else {
      if ($(target).hasClass("show")) {
        $(target).collapse('hide');
        toggler.html("Show Code <i class='fa fa-angle-down'></i>");
      } else {
        $(target).collapse('show');
        toggler.html("Hide Code <i class='fa fa-angle-up'></i>");
      }
    }
  }

  var firstHId;
  var lastHId;

  function generateStructure() {
    
    $(".bootstrap-example-wrapper :header").not("section .row .col-12 :header").each(function (index) {
      $(this).attr("id", "section-" + index);
      $(this).addClass("override-style");
    });

    firstHId = $(".content h1").first().attr("id");
    lastHId = $(".content h2").last().attr("id");

    var htmlList = "";

    $(".bootstrap-example-wrapper :header").not("section .row .col-12 :header").each(function (index) {

      if ($(this)[0].tagName == "H1") {

        if ($(this).attr("id") == firstHId) {

          htmlList += "<li class='pt-2'><a data-toggle='collapse' aria-expanded='false' class='dropdown-toggle' href='#dropdown-" + index + "'>" + $(this).text() + "</a><ul class='collapse list-unstyled' id='dropdown-" + index + "'>";

        } else {

          htmlList += "</ul></li><li class='pt-2'><a data-toggle='collapse' aria-expanded='false' class='dropdown-toggle' href='#dropdown-" + index + "'>" + $(this).text() + "</a><ul class='collapse list-unstyled' id='dropdown-" + index + "'>";

        }

      } else if ($(this)[0].tagName == "H2") {
        if ($(this).attr("id") == lastHId) {
          htmlList += "<li class='pl-3 py-1'><a class='anchor' href='#section-" + index + "'>" + $(this).text() + "</a></li></ul></li>";
        } else {
          htmlList += "<li class='pl-3 py-1'><a class='anchor' href='#section-" + index + "'>" + $(this).text() + "</a></li>";
        }
      } else if ($(this)[0].tagName == "H3") {
        if ($(this).attr("id") == lastHId) {
          htmlList += "<li class='pl-3 py-1 ml-3'><a class='anchor' href='#section-" + index + "'>" + $(this).text() + "</a></li></ul></li>";
        } else {
          htmlList += "<li class='pl-3 py-1 ml-3'><a class='anchor' href='#section-" + index + "'>" + $(this).text() + "</a></li>";
        }
      } else if ($(this)[0].tagName == "H4") {
        if ($(this).attr("id") == lastHId) {
          htmlList += "<li class='pl-5 py-1 ml-5'><a class='anchor' href='#section-" + index + "'>" + $(this).text() + "</a></li></ul></li>";
        } else {
          htmlList += "<li class='pl-5 py-1 ml-5'><a class='anchor' href='#section-" + index + "'>" + $(this).text() + "</a></li>";
        }
      }
    });

    $(".bootstrap-example-wrapper :header").not("section .row .col-12 :header").each(function (index) {
      $(this).append(" <a href='#"+$(this).attr("id")+"'>#</a>");
    });

    $(".structure-list ul").append(htmlList);

    $(".structure-list ul li ul li").each(function(){
      if($(this).index() == 0){
        var href = "#section-"+ ($(this).find("a").attr("href").split("-")[1]-1);
        $(this).find("a").attr("href", href);
      }
    });
  }
</script>

<div class="row">
  <div class="structure-list col-12 col-md-2 pl-2 pl-md-5 position-relative">
    <ul class="list-unstyled components position-fixed py-2 py-md-4">

    </ul>
  </div>
  <div class="content col-12 col-md-10">
    <div class="container">
    <a class="btn btn-primary mr-2" href="#show-all" data-toggle="custom-collapse">Show all</a>
    <a class="btn btn-primary" href="#hide-all" data-toggle="custom-collapse">Hide all</a>


    <h1>Typography</h1>

    <section>
      <h2>Headings</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <h1>Nadpis 1</h1>
          <h2>Nadpis 2</h2>
          <h3>Nadpis 3</h3>
          <h4>Nadpis 4</h4>
          <h5>Nadpis 5</h5>
          <h6>Nadpis 6</h6>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;h1&gt;Nadpis 1&lt;/h1&gt;
    &lt;h2&gt;Nadpis 2&lt;/h2&gt;
    &lt;h3&gt;Nadpis 3&lt;/h3&gt;
    &lt;h4&gt;Nadpis 4&lt;/h4&gt;
    &lt;h5&gt;Nadpis 5&lt;/h5&gt;
    &lt;h6&gt;Nadpis 6&lt;/h6&gt;
    </code>
  </pre>

      <h3>Customizing headings</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <h3>
            Fancy display heading
            <small class="text-muted">With faded secondary text</small>
          </h3>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;h3&gt;
      Fancy display heading
      &lt;small class=&quot;text-muted&quot;&gt;With faded secondary text&lt;/small&gt;
    &lt;/h3&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Display headings</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <h1 class="display-1">Display 1</h1>
          <h1 class="display-2">Display 2</h1>
          <h1 class="display-3">Display 3</h1>
          <h1 class="display-4">Display 4</h1>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;h1 class=&quot;display-1&quot;&gt;Display 1&lt;/h1&gt;
    &lt;h1 class=&quot;display-2&quot;&gt;Display 2&lt;/h1&gt;
    &lt;h1 class=&quot;display-3&quot;&gt;Display 3&lt;/h1&gt;
    &lt;h1 class=&quot;display-4&quot;&gt;Display 4&lt;/h1&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Lead</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <p class="lead">
            Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor. Duis mollis, est non commodo luctus.
          </p>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;p class=&quot;lead&quot;&gt;
      Vivamus sagittis lacus vel augue laoreet rutrum faucibus dolor auctor. Duis mollis, est non commodo luctus.
    &lt;/p&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Inline text elements</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <p>You can use the mark tag to <mark>highlight</mark> text.</p>
          <p><del>This line of text is meant to be treated as deleted text.</del></p>
          <p><s>This line of text is meant to be treated as no longer accurate.</s></p>
          <p><ins>This line of text is meant to be treated as an addition to the document.</ins></p>
          <p><u>This line of text will render as underlined</u></p>
          <p><small>This line of text is meant to be treated as fine print.</small></p>
          <p><strong>This line rendered as bold text.</strong></p>
          <p><em>This line rendered as italicized text.</em></p>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;p&gt;You can use the mark tag to &lt;mark&gt;highlight&lt;/mark&gt; text.&lt;/p&gt;
    &lt;p&gt;&lt;del&gt;This line of text is meant to be treated as deleted text.&lt;/del&gt;&lt;/p&gt;
    &lt;p&gt;&lt;s&gt;This line of text is meant to be treated as no longer accurate.&lt;/s&gt;&lt;/p&gt;
    &lt;p&gt;&lt;ins&gt;This line of text is meant to be treated as an addition to the document.&lt;/ins&gt;&lt;/p&gt;
    &lt;p&gt;&lt;u&gt;This line of text will render as underlined&lt;/u&gt;&lt;/p&gt;
    &lt;p&gt;&lt;small&gt;This line of text is meant to be treated as fine print.&lt;/small&gt;&lt;/p&gt;
    &lt;p&gt;&lt;strong&gt;This line rendered as bold text.&lt;/strong&gt;&lt;/p&gt;
    &lt;p&gt;&lt;em&gt;This line rendered as italicized text.&lt;/em&gt;&lt;/p&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Abbreviations</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <p><abbr title="attribute">attr</abbr></p>
          <p><abbr title="HyperText Markup Language" class="initialism">HTML</abbr></p>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;p&gt;&lt;abbr title=&quot;attribute&quot;&gt;attr&lt;/abbr&gt;&lt;/p&gt;
    &lt;p&gt;&lt;abbr title=&quot;HyperText Markup Language&quot; class=&quot;initialism&quot;&gt;HTML&lt;/abbr&gt;&lt;/p&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Blockquotes</h2>
      <h3>Naming a source</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <blockquote class="blockquote">
            <p class="mb-0">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.</p>
            <footer class="blockquote-footer">Someone famous in <cite title="Source Title">Source Title</cite></footer>
          </blockquote>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;blockquote class=&quot;blockquote&quot;&gt;
      &lt;p class=&quot;mb-0&quot;&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.&lt;/p&gt;
      &lt;footer class=&quot;blockquote-footer&quot;&gt;Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;&lt;/footer&gt;
    &lt;/blockquote&gt;
    </code>
  </pre>

      <h3>Alignment</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <blockquote class="blockquote text-center">
            <p class="mb-0">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.</p>
            <footer class="blockquote-footer">Someone famous in <cite title="Source Title">Source Title</cite></footer>
          </blockquote>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;blockquote class=&quot;blockquote text-center&quot;&gt;
      &lt;p class=&quot;mb-0&quot;&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.&lt;/p&gt;
      &lt;footer class=&quot;blockquote-footer&quot;&gt;Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;&lt;/footer&gt;
    &lt;/blockquote&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <blockquote class="blockquote text-right">
            <p class="mb-0">Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.</p>
            <footer class="blockquote-footer">Someone famous in <cite title="Source Title">Source Title</cite></footer>
          </blockquote>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;blockquote class=&quot;blockquote text-right&quot;&gt;
      &lt;p class=&quot;mb-0&quot;&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.&lt;/p&gt;
      &lt;footer class=&quot;blockquote-footer&quot;&gt;Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;&lt;/footer&gt;
    &lt;/blockquote&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Lists</h2>
      <h3>Unstyled</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <ul class="list-unstyled">
            <li>Lorem ipsum dolor sit amet</li>
            <li>Consectetur adipiscing elit</li>
            <li>Integer molestie lorem at massa</li>
            <li>Facilisis in pretium nisl aliquet</li>
            <li>Nulla volutpat aliquam velit
              <ul>
                <li>Phasellus iaculis neque</li>
                <li>Purus sodales ultricies</li>
                <li>Vestibulum laoreet porttitor sem</li>
                <li>Ac tristique libero volutpat at</li>
              </ul>
            </li>
            <li>Faucibus porta lacus fringilla vel</li>
            <li>Aenean sit amet erat nunc</li>
            <li>Eget porttitor lorem</li>
          </ul>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-unstyled&quot;&gt;
      &lt;li&gt;Lorem ipsum dolor sit amet&lt;/li&gt;
      &lt;li&gt;Consectetur adipiscing elit&lt;/li&gt;
      &lt;li&gt;Integer molestie lorem at massa&lt;/li&gt;
      &lt;li&gt;Facilisis in pretium nisl aliquet&lt;/li&gt;
      &lt;li&gt;Nulla volutpat aliquam velit
        &lt;ul&gt;
          &lt;li&gt;Phasellus iaculis neque&lt;/li&gt;
          &lt;li&gt;Purus sodales ultricies&lt;/li&gt;
          &lt;li&gt;Vestibulum laoreet porttitor sem&lt;/li&gt;
          &lt;li&gt;Ac tristique libero volutpat at&lt;/li&gt;
        &lt;/ul&gt;
      &lt;/li&gt;
      &lt;li&gt;Faucibus porta lacus fringilla vel&lt;/li&gt;
      &lt;li&gt;Aenean sit amet erat nunc&lt;/li&gt;
      &lt;li&gt;Eget porttitor lorem&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>

      <h3>Inline</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <ul class="list-inline">
            <li class="list-inline-item">Lorem ipsum</li>
            <li class="list-inline-item">Phasellus iaculis</li>
            <li class="list-inline-item">Nulla volutpat</li>
          </ul>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;ul class=&quot;list-inline&quot;&gt;
      &lt;li class=&quot;list-inline-item&quot;&gt;Lorem ipsum&lt;/li&gt;
      &lt;li class=&quot;list-inline-item&quot;&gt;Phasellus iaculis&lt;/li&gt;
      &lt;li class=&quot;list-inline-item&quot;&gt;Nulla volutpat&lt;/li&gt;
    &lt;/ul&gt;
    </code>
  </pre>

  <h3>Order List</h3>
  <hr>
  <div class="row">
      <div class="col-12">
        <ol>
          <li>Lorem ipsum dolor sit amet</li>
          <li>Consectetur adipiscing elit</li>
          <li>Integer molestie lorem at massa</li>
          <li>Facilisis in pretium nisl aliquet</li>
          <li>Nulla volutpat aliquam velit
            <ol>
              <li>Phasellus iaculis neque</li>
              <li>Purus sodales ultricies</li>
              <li>Vestibulum laoreet porttitor sem</li>
              <li>Ac tristique libero volutpat at</li>
            </ol>
          </li>
          <li>Faucibus porta lacus fringilla vel</li>
          <li>Aenean sit amet erat nunc</li>
          <li>Eget porttitor lorem</li>
        </ol>
      </div>
    </div>
    <pre class="m-0 col-12">
  <code class="collapse html">
  &lt;ol&gt;
    &lt;li&gt;Lorem ipsum dolor sit amet&lt;/li&gt;
    &lt;li&gt;Consectetur adipiscing elit&lt;/li&gt;
    &lt;li&gt;Integer molestie lorem at massa&lt;/li&gt;
    &lt;li&gt;Facilisis in pretium nisl aliquet&lt;/li&gt;
    &lt;li&gt;Nulla volutpat aliquam velit
      &lt;ol&gt;
        &lt;li&gt;Phasellus iaculis neque&lt;/li&gt;
        &lt;li&gt;Purus sodales ultricies&lt;/li&gt;
        &lt;li&gt;Vestibulum laoreet porttitor sem&lt;/li&gt;
        &lt;li&gt;Ac tristique libero volutpat at&lt;/li&gt;
      &lt;/ol&gt;
    &lt;/li&gt;
    &lt;li&gt;Faucibus porta lacus fringilla vel&lt;/li&gt;
    &lt;li&gt;Aenean sit amet erat nunc&lt;/li&gt;
    &lt;li&gt;Eget porttitor lorem&lt;/li&gt;
  &lt;/ol&gt;
  </code>
</pre>

      <h3>Description list alignment</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <dl class="row">
            <dt class="col-sm-3">Description lists</dt>
            <dd class="col-sm-9">A description list is perfect for defining terms.</dd>

            <dt class="col-sm-3">Euismod</dt>
            <dd class="col-sm-9">
              <p>Vestibulum id ligula porta felis euismod semper eget lacinia odio sem nec elit.</p>
              <p>Donec id elit non mi porta gravida at eget metus.</p>
            </dd>

            <dt class="col-sm-3">Malesuada porta</dt>
            <dd class="col-sm-9">Etiam porta sem malesuada magna mollis euismod.</dd>

            <dt class="col-sm-3 text-truncate">Truncated term is truncated</dt>
            <dd class="col-sm-9">Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum
              massa justo sit amet risus.</dd>

            <dt class="col-sm-3">Nesting</dt>
            <dd class="col-sm-9">
              <dl class="row">
                <dt class="col-sm-4">Nested definition list</dt>
                <dd class="col-sm-8">Aenean posuere, tortor sed cursus feugiat, nunc augue blandit nunc.</dd>
              </dl>
            </dd>
          </dl>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;dl class=&quot;row&quot;&gt;
      &lt;dt class=&quot;col-sm-3&quot;&gt;Description lists&lt;/dt&gt;
      &lt;dd class=&quot;col-sm-9&quot;&gt;A description list is perfect for defining terms.&lt;/dd&gt;

      &lt;dt class=&quot;col-sm-3&quot;&gt;Euismod&lt;/dt&gt;
      &lt;dd class=&quot;col-sm-9&quot;&gt;
        &lt;p&gt;Vestibulum id ligula porta felis euismod semper eget lacinia odio sem nec elit.&lt;/p&gt;
        &lt;p&gt;Donec id elit non mi porta gravida at eget metus.&lt;/p&gt;
      &lt;/dd&gt;

      &lt;dt class=&quot;col-sm-3&quot;&gt;Malesuada porta&lt;/dt&gt;
      &lt;dd class=&quot;col-sm-9&quot;&gt;Etiam porta sem malesuada magna mollis euismod.&lt;/dd&gt;

      &lt;dt class=&quot;col-sm-3 text-truncate&quot;&gt;Truncated term is truncated&lt;/dt&gt;
      &lt;dd class=&quot;col-sm-9&quot;&gt;Fusce dapibus, tellus ac cursus commodo, tortor mauris condimentum nibh, ut fermentum massa justo sit amet risus.&lt;/dd&gt;

      &lt;dt class=&quot;col-sm-3&quot;&gt;Nesting&lt;/dt&gt;
      &lt;dd class=&quot;col-sm-9&quot;&gt;
        &lt;dl class=&quot;row&quot;&gt;
          &lt;dt class=&quot;col-sm-4&quot;&gt;Nested definition list&lt;/dt&gt;
          &lt;dd class=&quot;col-sm-8&quot;&gt;Aenean posuere, tortor sed cursus feugiat, nunc augue blandit nunc.&lt;/dd&gt;
        &lt;/dl&gt;
      &lt;/dd&gt;
    &lt;/dl&gt;
    </code>
  </pre>
    </section>

    <h1>Code</h1>

    <section>
      <h2>Inline code</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          For example, <code>&lt;section&gt;</code> should be wrapped as inline.
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    For example, &lt;code&gt;&amp;lt;section&amp;gt;&lt;/code&gt; should be wrapped as inline.
    </code>
  </pre>
    </section>

    <section>
      <h2>Code blocks</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <pre class="no-highlight">

      <code>&lt;p&gt;Sample text here...&lt;/p&gt;
      &lt;p&gt;And another line of sample text here...&lt;/p&gt;
      </code></pre>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;pre&gt;&lt;code&gt;&amp;lt;p&amp;gt;Sample text here...&amp;lt;/p&amp;gt;
    &amp;lt;p&amp;gt;And another line of sample text here...&amp;lt;/p&amp;gt;
    &lt;/code&gt;&lt;/pre&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Variables</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <var>y</var> = <var>m</var><var>x</var> + <var>b</var>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;var&gt;y&lt;/var&gt; = &lt;var&gt;m&lt;/var&gt;&lt;var&gt;x&lt;/var&gt; + &lt;var&gt;b&lt;/var&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>User input</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          To switch directories, type <kbd>cd</kbd> followed by the name of the directory.<br>
          To edit settings, press <kbd><kbd>ctrl</kbd> + <kbd>,</kbd></kbd>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    To switch directories, type &lt;kbd&gt;cd&lt;/kbd&gt; followed by the name of the directory.&lt;br&gt;
    To edit settings, press &lt;kbd&gt;&lt;kbd&gt;ctrl&lt;/kbd&gt; + &lt;kbd&gt;,&lt;/kbd&gt;&lt;/kbd&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Sample output</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <samp>This text is meant to be treated as sample output from a computer program.</samp>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;samp&gt;This text is meant to be treated as sample output from a computer program.&lt;/samp&gt;
    </code>
  </pre>
    </section>

    <h1>Tables</h1>

    <section>
      <h2>Examples</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Table head options</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table">
            <thead class="thead-dark">
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>

          <table class="table">
            <thead class="thead-light">
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table&quot;&gt;
      &lt;thead class=&quot;thead-dark&quot;&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;

    &lt;table class=&quot;table&quot;&gt;
      &lt;thead class=&quot;thead-light&quot;&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Striped rows</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table table-striped">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-striped&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark table-striped">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark table-striped&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Bordered table</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table table-bordered">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-bordered&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark table-bordered">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark table-bordered&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Borderless table</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table table-borderless">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-borderless&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark table-borderless">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark table-borderless&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Hoverable rows</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table table-hover">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-hover&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark table-hover">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark table-hover&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Small table</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table table-sm">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-sm&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark table-sm">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td colspan="2">Larry the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark table-sm&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td colspan=&quot;2&quot;&gt;Larry the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Contextual classes</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table">
            <thead>
              <tr>
                <th scope="col">Class</th>
                <th scope="col">Heading</th>
                <th scope="col">Heading</th>
              </tr>
            </thead>
            <tbody>
              <tr class="table-active">
                <th scope="row">Active</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr>
                <th scope="row">Default</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-primary">
                <th scope="row">Primary</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-secondary">
                <th scope="row">Secondary</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-success">
                <th scope="row">Success</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-danger">
                <th scope="row">Danger</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-warning">
                <th scope="row">Warning</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-info">
                <th scope="row">Info</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-light">
                <th scope="row">Light</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="table-dark">
                <th scope="row">Dark</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;Class&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr class=&quot;table-active&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Active&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;Default&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-primary&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Primary&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-secondary&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Secondary&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-success&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Success&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-danger&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Danger&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-warning&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Warning&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-info&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Info&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-light&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Light&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;table-dark&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;Dark&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <table class="table table-dark">
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">Heading</th>
                <th scope="col">Heading</th>
              </tr>
            </thead>
            <tbody>
              <tr class="bg-primary">
                <th scope="row">1</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="bg-success">
                <th scope="row">3</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr>
                <th scope="row">4</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="bg-info">
                <th scope="row">5</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr>
                <th scope="row">6</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="bg-warning">
                <th scope="row">7</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr>
                <th scope="row">8</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
              <tr class="bg-danger">
                <th scope="row">9</th>
                <td>Cell</td>
                <td>Cell</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table table-dark&quot;&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr class=&quot;bg-primary&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;bg-success&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;4&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;bg-info&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;5&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;6&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;bg-warning&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;7&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;8&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr class=&quot;bg-danger&quot;&gt;
          &lt;th scope=&quot;row&quot;&gt;9&lt;/th&gt;
          &lt;td&gt;Cell&lt;/td&gt;
          &lt;td&gt;Cell&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Captions</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <table class="table">
            <caption>List of users</caption>
            <thead>
              <tr>
                <th scope="col">#</th>
                <th scope="col">First</th>
                <th scope="col">Last</th>
                <th scope="col">Handle</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <th scope="row">1</th>
                <td>Mark</td>
                <td>Otto</td>
                <td>@mdo</td>
              </tr>
              <tr>
                <th scope="row">2</th>
                <td>Jacob</td>
                <td>Thornton</td>
                <td>@fat</td>
              </tr>
              <tr>
                <th scope="row">3</th>
                <td>Larry</td>
                <td>the Bird</td>
                <td>@twitter</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;table class=&quot;table&quot;&gt;
      &lt;caption&gt;List of users&lt;/caption&gt;
      &lt;thead&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;First&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Last&lt;/th&gt;
          &lt;th scope=&quot;col&quot;&gt;Handle&lt;/th&gt;
        &lt;/tr&gt;
      &lt;/thead&gt;
      &lt;tbody&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
          &lt;td&gt;Mark&lt;/td&gt;
          &lt;td&gt;Otto&lt;/td&gt;
          &lt;td&gt;@mdo&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
          &lt;td&gt;Jacob&lt;/td&gt;
          &lt;td&gt;Thornton&lt;/td&gt;
          &lt;td&gt;@fat&lt;/td&gt;
        &lt;/tr&gt;
        &lt;tr&gt;
          &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
          &lt;td&gt;Larry&lt;/td&gt;
          &lt;td&gt;the Bird&lt;/td&gt;
          &lt;td&gt;@twitter&lt;/td&gt;
        &lt;/tr&gt;
      &lt;/tbody&gt;
    &lt;/table&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Responsive tables</h2>
      <h3>Always responsive</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="table-responsive">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">1</th>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                </tr>
                <tr>
                  <th scope="row">2</th>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                </tr>
                <tr>
                  <th scope="row">3</th>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;table-responsive&quot;&gt;
      &lt;table class=&quot;table&quot;&gt;
        &lt;thead&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
          &lt;/tr&gt;
        &lt;/thead&gt;
        &lt;tbody&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
          &lt;/tr&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
          &lt;/tr&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
          &lt;/tr&gt;
        &lt;/tbody&gt;
      &lt;/table&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Breakpoint specific</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="table-responsive-md">
            <table class="table">
              <thead>
                <tr>
                  <th scope="col">#</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                  <th scope="col">Heading</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <th scope="row">1</th>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                </tr>
                <tr>
                  <th scope="row">2</th>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                </tr>
                <tr>
                  <th scope="row">3</th>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                  <td>Cell</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;table-responsive-sm|md|lg|xl&quot;&gt;
      &lt;table class=&quot;table&quot;&gt;
        &lt;thead&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;col&quot;&gt;#&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
            &lt;th scope=&quot;col&quot;&gt;Heading&lt;/th&gt;
          &lt;/tr&gt;
        &lt;/thead&gt;
        &lt;tbody&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;row&quot;&gt;1&lt;/th&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
          &lt;/tr&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;row&quot;&gt;2&lt;/th&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
          &lt;/tr&gt;
          &lt;tr&gt;
            &lt;th scope=&quot;row&quot;&gt;3&lt;/th&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
            &lt;td&gt;Cell&lt;/td&gt;
          &lt;/tr&gt;
        &lt;/tbody&gt;
      &lt;/table&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <h1>Alerts</h1>

    <section>
      <h2>Examples</h2>
      <h3>Link Color</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="alert alert-primary" role="alert">
            A simple primary alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you
            like.
          </div>
          <div class="alert alert-secondary" role="alert">
            A simple secondary alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you
            like.
          </div>
          <div class="alert alert-success" role="alert">
            A simple success alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you
            like.
          </div>
          <div class="alert alert-danger" role="alert">
            A simple danger alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you like.
          </div>
          <div class="alert alert-warning" role="alert">
            A simple warning alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you
            like.
          </div>
          <div class="alert alert-info" role="alert">
            A simple info alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you like.
          </div>
          <div class="alert alert-light" role="alert">
            A simple light alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you like.
          </div>
          <div class="alert alert-dark" role="alert">
            A simple dark alert with <a href="#" class="alert-link">an example link</a>. Give it a click if you like.
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;alert alert-primary&quot; role=&quot;alert&quot;&gt;
      A simple primary alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-secondary&quot; role=&quot;alert&quot;&gt;
      A simple secondary alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-success&quot; role=&quot;alert&quot;&gt;
      A simple success alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-danger&quot; role=&quot;alert&quot;&gt;
      A simple danger alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-warning&quot; role=&quot;alert&quot;&gt;
      A simple warning alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-info&quot; role=&quot;alert&quot;&gt;
      A simple info alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-light&quot; role=&quot;alert&quot;&gt;
      A simple light alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    &lt;div class=&quot;alert alert-dark&quot; role=&quot;alert&quot;&gt;
      A simple dark alert with &lt;a href=&quot;#&quot; class=&quot;alert-link&quot;&gt;an example link&lt;/a&gt;. Give it a click if you like.
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Additional content</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="alert alert-success" role="alert">
            <h4 class="alert-heading">Well done!</h4>
            <p>Aww yeah, you successfully read this important alert message. This example text is going to run a bit
              longer
              so that you can see how spacing within an alert works with this kind of content.</p>
            <hr>
            <p class="mb-0">Whenever you need to, be sure to use margin utilities to keep things nice and tidy.</p>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;alert alert-success&quot; role=&quot;alert&quot;&gt;
      &lt;h4 class=&quot;alert-heading&quot;&gt;Well done!&lt;/h4&gt;
      &lt;p&gt;Aww yeah, you successfully read this important alert message. This example text is going to run a bit longer so that you can see how spacing within an alert works with this kind of content.&lt;/p&gt;
      &lt;hr&gt;
      &lt;p class=&quot;mb-0&quot;&gt;Whenever you need to, be sure to use margin utilities to keep things nice and tidy.&lt;/p&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Dismissing</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="alert alert-warning alert-dismissible fade show" role="alert">
            <strong>Holy guacamole!</strong> You should check in on some of those fields below.
            <button type="button" class="close" data-dismiss="alert" aria-label="Close">
              <span aria-hidden="true">&times;</span>
            </button>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;alert alert-warning alert-dismissible fade show&quot; role=&quot;alert&quot;&gt;
      &lt;strong&gt;Holy guacamole!&lt;/strong&gt; You should check in on some of those fields below.
      &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;alert&quot; aria-label=&quot;Close&quot;&gt;
        &lt;span aria-hidden=&quot;true&quot;&gt;&amp;times;&lt;/span&gt;
      &lt;/button&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <h1>Badges</h1>

    <section>
      <h2>Example</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <h1>Example heading <span class="badge badge-secondary">New</span></h1>
          <h2>Example heading <span class="badge badge-secondary">New</span></h2>
          <h3>Example heading <span class="badge badge-secondary">New</span></h3>
          <h4>Example heading <span class="badge badge-secondary">New</span></h4>
          <h5>Example heading <span class="badge badge-secondary">New</span></h5>
          <h6>Example heading <span class="badge badge-secondary">New</span></h6>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;h1&gt;Example heading &lt;span class=&quot;badge badge-secondary&quot;&gt;New&lt;/span&gt;&lt;/h1&gt;
    &lt;h2&gt;Example heading &lt;span class=&quot;badge badge-secondary&quot;&gt;New&lt;/span&gt;&lt;/h2&gt;
    &lt;h3&gt;Example heading &lt;span class=&quot;badge badge-secondary&quot;&gt;New&lt;/span&gt;&lt;/h3&gt;
    &lt;h4&gt;Example heading &lt;span class=&quot;badge badge-secondary&quot;&gt;New&lt;/span&gt;&lt;/h4&gt;
    &lt;h5&gt;Example heading &lt;span class=&quot;badge badge-secondary&quot;&gt;New&lt;/span&gt;&lt;/h5&gt;
    &lt;h6&gt;Example heading &lt;span class=&quot;badge badge-secondary&quot;&gt;New&lt;/span&gt;&lt;/h6&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary">
            Notifications <span class="badge badge-light">4</span>
          </button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot;&gt;
      Notifications &lt;span class=&quot;badge badge-light&quot;&gt;4&lt;/span&gt;
    &lt;/button&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Contextual variations</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <span class="badge badge-primary">Primary</span>
          <span class="badge badge-secondary">Secondary</span>
          <span class="badge badge-success">Success</span>
          <span class="badge badge-danger">Danger</span>
          <span class="badge badge-warning">Warning</span>
          <span class="badge badge-info">Info</span>
          <span class="badge badge-light">Light</span>
          <span class="badge badge-dark">Dark</span>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;span class=&quot;badge badge-primary&quot;&gt;Primary&lt;/span&gt;
    &lt;span class=&quot;badge badge-secondary&quot;&gt;Secondary&lt;/span&gt;
    &lt;span class=&quot;badge badge-success&quot;&gt;Success&lt;/span&gt;
    &lt;span class=&quot;badge badge-danger&quot;&gt;Danger&lt;/span&gt;
    &lt;span class=&quot;badge badge-warning&quot;&gt;Warning&lt;/span&gt;
    &lt;span class=&quot;badge badge-info&quot;&gt;Info&lt;/span&gt;
    &lt;span class=&quot;badge badge-light&quot;&gt;Light&lt;/span&gt;
    &lt;span class=&quot;badge badge-dark&quot;&gt;Dark&lt;/span&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Pill badges</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <span class="badge badge-pill badge-primary">Primary</span>
          <span class="badge badge-pill badge-secondary">Secondary</span>
          <span class="badge badge-pill badge-success">Success</span>
          <span class="badge badge-pill badge-danger">Danger</span>
          <span class="badge badge-pill badge-warning">Warning</span>
          <span class="badge badge-pill badge-info">Info</span>
          <span class="badge badge-pill badge-light">Light</span>
          <span class="badge badge-pill badge-dark">Dark</span>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;span class=&quot;badge badge-pill badge-primary&quot;&gt;Primary&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-secondary&quot;&gt;Secondary&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-success&quot;&gt;Success&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-danger&quot;&gt;Danger&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-warning&quot;&gt;Warning&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-info&quot;&gt;Info&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-light&quot;&gt;Light&lt;/span&gt;
    &lt;span class=&quot;badge badge-pill badge-dark&quot;&gt;Dark&lt;/span&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Links</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <a href="#" class="badge badge-primary">Primary</a>
          <a href="#" class="badge badge-secondary">Secondary</a>
          <a href="#" class="badge badge-success">Success</a>
          <a href="#" class="badge badge-danger">Danger</a>
          <a href="#" class="badge badge-warning">Warning</a>
          <a href="#" class="badge badge-info">Info</a>
          <a href="#" class="badge badge-light">Light</a>
          <a href="#" class="badge badge-dark">Dark</a>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;a href=&quot;#&quot; class=&quot;badge badge-primary&quot;&gt;Primary&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-secondary&quot;&gt;Secondary&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-success&quot;&gt;Success&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-danger&quot;&gt;Danger&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-warning&quot;&gt;Warning&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-info&quot;&gt;Info&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-light&quot;&gt;Light&lt;/a&gt;
    &lt;a href=&quot;#&quot; class=&quot;badge badge-dark&quot;&gt;Dark&lt;/a&gt;
    </code>
  </pre>
    </section>

    <h1>Breadcrumb</h1>

    <section>
      <h2>Example</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
              <li class="breadcrumb-item active" aria-current="page">Home</li>
            </ol>
          </nav>

          <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
              <li class="breadcrumb-item"><a href="#">Home</a></li>
              <li class="breadcrumb-item active" aria-current="page">Library</li>
            </ol>
          </nav>

          <nav aria-label="breadcrumb">
            <ol class="breadcrumb">
              <li class="breadcrumb-item"><a href="#">Home</a></li>
              <li class="breadcrumb-item"><a href="#">Library</a></li>
              <li class="breadcrumb-item active" aria-current="page">Data</li>
            </ol>
          </nav>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;nav aria-label=&quot;breadcrumb&quot;&gt;
      &lt;ol class=&quot;breadcrumb&quot;&gt;
        &lt;li class=&quot;breadcrumb-item active&quot; aria-current=&quot;page&quot;&gt;Home&lt;/li&gt;
      &lt;/ol&gt;
    &lt;/nav&gt;

    &lt;nav aria-label=&quot;breadcrumb&quot;&gt;
      &lt;ol class=&quot;breadcrumb&quot;&gt;
        &lt;li class=&quot;breadcrumb-item&quot;&gt;&lt;a href=&quot;#&quot;&gt;Home&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;breadcrumb-item active&quot; aria-current=&quot;page&quot;&gt;Library&lt;/li&gt;
      &lt;/ol&gt;
    &lt;/nav&gt;

    &lt;nav aria-label=&quot;breadcrumb&quot;&gt;
      &lt;ol class=&quot;breadcrumb&quot;&gt;
        &lt;li class=&quot;breadcrumb-item&quot;&gt;&lt;a href=&quot;#&quot;&gt;Home&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;breadcrumb-item&quot;&gt;&lt;a href=&quot;#&quot;&gt;Library&lt;/a&gt;&lt;/li&gt;
        &lt;li class=&quot;breadcrumb-item active&quot; aria-current=&quot;page&quot;&gt;Data&lt;/li&gt;
      &lt;/ol&gt;
    &lt;/nav&gt;
    </code>
  </pre>
    </section>

    <h1>Card</h1>

    <section>
      <h2>Content types</h2>
      <h3>Body</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card">
            <div class="card-body">
              This is some text within a card body.
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        This is some text within a card body.
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Titles, text, and links</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card" >
            <div class="card-body">
              <h5 class="card-title">Card title</h5>
              <h6 class="card-subtitle mb-2 text-muted">Card subtitle</h6>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
              <a href="#" class="card-link">Card link</a>
              <a href="#" class="card-link">Another link</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
        &lt;h6 class=&quot;card-subtitle mb-2 text-muted&quot;&gt;Card subtitle&lt;/h6&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;card-link&quot;&gt;Card link&lt;/a&gt;
        &lt;a href=&quot;#&quot; class=&quot;card-link&quot;&gt;Another link&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>List groups</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card" >
            <ul class="list-group list-group-flush">
              <li class="list-group-item">Cras justo odio</li>
              <li class="list-group-item">Dapibus ac facilisis in</li>
              <li class="list-group-item">Vestibulum at eros</li>
            </ul>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;ul class=&quot;list-group list-group-flush&quot;&gt;
        &lt;li class=&quot;list-group-item&quot;&gt;Cras justo odio&lt;/li&gt;
        &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;
        &lt;li class=&quot;list-group-item&quot;&gt;Vestibulum at eros&lt;/li&gt;
      &lt;/ul&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <div class="card" >
            <div class="card-header">
              Featured
            </div>
            <ul class="list-group list-group-flush">
              <li class="list-group-item">Cras justo odio</li>
              <li class="list-group-item">Dapibus ac facilisis in</li>
              <li class="list-group-item">Vestibulum at eros</li>
            </ul>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;
        Featured
      &lt;/div&gt;
      &lt;ul class=&quot;list-group list-group-flush&quot;&gt;
        &lt;li class=&quot;list-group-item&quot;&gt;Cras justo odio&lt;/li&gt;
        &lt;li class=&quot;list-group-item&quot;&gt;Dapibus ac facilisis in&lt;/li&gt;
        &lt;li class=&quot;list-group-item&quot;&gt;Vestibulum at eros&lt;/li&gt;
      &lt;/ul&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Header and footer</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card">
            <div class="card-header">
              Featured
            </div>
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;
        Featured
      &lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <div class="card">
            <div class="card-header">
              Quote
            </div>
            <div class="card-body">
              <blockquote class="blockquote mb-0">
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.</p>
                <footer class="blockquote-footer">Someone famous in <cite title="Source Title">Source Title</cite></footer>
              </blockquote>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;
        Quote
      &lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;blockquote class=&quot;blockquote mb-0&quot;&gt;
          &lt;p&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.&lt;/p&gt;
          &lt;footer class=&quot;blockquote-footer&quot;&gt;Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;&lt;/footer&gt;
        &lt;/blockquote&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <div class="card text-center">
            <div class="card-header">
              Featured
            </div>
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
            <div class="card-footer text-muted">
              2 days ago
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card text-center&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;
        Featured
      &lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card-footer text-muted&quot;&gt;
        2 days ago
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Sizing</h2>
      <h3>Using grid markup</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="row">
            <div class="col-sm-6">
              <div class="card">
                <div class="card-body">
                  <h5 class="card-title">Special title treatment</h5>
                  <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
                  <a href="#" class="btn btn-primary">Go somewhere</a>
                </div>
              </div>
            </div>
            <div class="col-sm-6">
              <div class="card">
                <div class="card-body">
                  <h5 class="card-title">Special title treatment</h5>
                  <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
                  <a href="#" class="btn btn-primary">Go somewhere</a>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;row&quot;&gt;
      &lt;div class=&quot;col-sm-6&quot;&gt;
        &lt;div class=&quot;card&quot;&gt;
          &lt;div class=&quot;card-body&quot;&gt;
            &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
            &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
            &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;col-sm-6&quot;&gt;
        &lt;div class=&quot;card&quot;&gt;
          &lt;div class=&quot;card-body&quot;&gt;
            &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
            &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
            &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Using utilities</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card w-75">
            <div class="card-body">
              <h5 class="card-title">Card title</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Button</a>
            </div>
          </div>

          <div class="card w-50">
            <div class="card-body">
              <h5 class="card-title">Card title</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Button</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card w-75&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Button&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;div class=&quot;card w-50&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Button&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Text alignment</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card" >
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
          </div>

          <div class="card text-center" >
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
          </div>

          <div class="card text-right" >
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;div class=&quot;card text-center&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;div class=&quot;card text-right&quot;&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Navigation</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card text-center">
            <div class="card-header">
              <ul class="nav nav-tabs card-header-tabs">
                <li class="nav-item">
                  <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                  <a class="nav-link disabled" href="#">Disabled</a>
                </li>
              </ul>
            </div>
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card text-center&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;
        &lt;ul class=&quot;nav nav-tabs card-header-tabs&quot;&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
          &lt;/li&gt;
        &lt;/ul&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <div class="card text-center">
            <div class="card-header">
              <ul class="nav nav-pills card-header-pills">
                <li class="nav-item">
                  <a class="nav-link active" href="#">Active</a>
                </li>
                <li class="nav-item">
                  <a class="nav-link" href="#">Link</a>
                </li>
                <li class="nav-item">
                  <a class="nav-link disabled" href="#">Disabled</a>
                </li>
              </ul>
            </div>
            <div class="card-body">
              <h5 class="card-title">Special title treatment</h5>
              <p class="card-text">With supporting text below as a natural lead-in to additional content.</p>
              <a href="#" class="btn btn-primary">Go somewhere</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card text-center&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;
        &lt;ul class=&quot;nav nav-pills card-header-pills&quot;&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link active&quot; href=&quot;#&quot;&gt;Active&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link&quot; href=&quot;#&quot;&gt;Link&lt;/a&gt;
          &lt;/li&gt;
          &lt;li class=&quot;nav-item&quot;&gt;
            &lt;a class=&quot;nav-link disabled&quot; href=&quot;#&quot;&gt;Disabled&lt;/a&gt;
          &lt;/li&gt;
        &lt;/ul&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Special title treatment&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;With supporting text below as a natural lead-in to additional content.&lt;/p&gt;
        &lt;a href=&quot;#&quot; class=&quot;btn btn-primary&quot;&gt;Go somewhere&lt;/a&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Card styles</h2>
      <h3>Background and color</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card text-white bg-primary mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Primary card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card text-white bg-secondary mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Secondary card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card text-white bg-success mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Success card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card text-white bg-danger mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Danger card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card text-white bg-warning mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Warning card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card text-white bg-info mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Info card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card bg-light mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Light card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card text-white bg-dark mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Dark card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card text-white bg-primary mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Primary card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card text-white bg-secondary mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Secondary card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card text-white bg-success mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Success card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card text-white bg-danger mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Danger card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card text-white bg-warning mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Warning card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card text-white bg-info mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Info card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card bg-light mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Light card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card text-white bg-dark mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Dark card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Border</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card border-primary mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-primary">
              <h5 class="card-title">Primary card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-secondary mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-secondary">
              <h5 class="card-title">Secondary card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-success mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-success">
              <h5 class="card-title">Success card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-danger mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-danger">
              <h5 class="card-title">Danger card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-warning mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-warning">
              <h5 class="card-title">Warning card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-info mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-info">
              <h5 class="card-title">Info card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-light mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body">
              <h5 class="card-title">Light card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
          <div class="card border-dark mb-3" >
            <div class="card-header">Header</div>
            <div class="card-body text-dark">
              <h5 class="card-title">Dark card title</h5>
              <p class="card-text">Some quick example text to build on the card title and make up the bulk of the
                card's
                content.</p>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card border-primary mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-primary&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Primary card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-secondary mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-secondary&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Secondary card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-success mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-success&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Success card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-danger mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-danger&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Danger card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-warning mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-warning&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Warning card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-info mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-info&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Info card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-light mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Light card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    &lt;div class=&quot;card border-dark mb-3&quot;&gt;
      &lt;div class=&quot;card-header&quot;&gt;Header&lt;/div&gt;
      &lt;div class=&quot;card-body text-dark&quot;&gt;
        &lt;h5 class=&quot;card-title&quot;&gt;Dark card title&lt;/h5&gt;
        &lt;p class=&quot;card-text&quot;&gt;Some quick example text to build on the card title and make up the bulk of the card's content.&lt;/p&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Card layout</h2>
      <h3>Card groups</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card-group">
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This is a wider card with supporting text below as a natural lead-in to additional
                  content. This content is a little bit longer.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This card has supporting text below as a natural lead-in to additional content.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This is a wider card with supporting text below as a natural lead-in to additional
                  content. This card has even longer content than the first to show that equal height action.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card-group&quot;&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This is a wider card with supporting text below as a natural lead-in to additional content. This content is a little bit longer.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This card has supporting text below as a natural lead-in to additional content.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This is a wider card with supporting text below as a natural lead-in to additional content. This card has even longer content than the first to show that equal height action.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Card decks</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card-deck">
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This is a longer card with supporting text below as a natural lead-in to
                  additional
                  content. This content is a little bit longer.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This card has supporting text below as a natural lead-in to additional content.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This is a wider card with supporting text below as a natural lead-in to additional
                  content. This card has even longer content than the first to show that equal height action.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card-deck&quot;&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This is a longer card with supporting text below as a natural lead-in to additional content. This content is a little bit longer.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This card has supporting text below as a natural lead-in to additional content.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This is a wider card with supporting text below as a natural lead-in to additional content. This card has even longer content than the first to show that equal height action.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Card columns</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="card-columns">
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title that wraps to a new line</h5>
                <p class="card-text">This is a longer card with supporting text below as a natural lead-in to
                  additional
                  content. This content is a little bit longer.</p>
              </div>
            </div>
            <div class="card p-3">
              <blockquote class="blockquote mb-0 card-body">
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.</p>
                <footer class="blockquote-footer">
                  <small class="text-muted">
                    Someone famous in <cite title="Source Title">Source Title</cite>
                  </small>
                </footer>
              </blockquote>
            </div>
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This card has supporting text below as a natural lead-in to additional content.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
            <div class="card bg-primary text-white text-center p-3">
              <blockquote class="blockquote mb-0">
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat.</p>
                <footer class="blockquote-footer">
                  <small>
                    Someone famous in <cite title="Source Title">Source Title</cite>
                  </small>
                </footer>
              </blockquote>
            </div>
            <div class="card text-center">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This card has a regular title and short paragraphy of text below it.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
            <div class="card p-3 text-right">
              <blockquote class="blockquote mb-0">
                <p>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.</p>
                <footer class="blockquote-footer">
                  <small class="text-muted">
                    Someone famous in <cite title="Source Title">Source Title</cite>
                  </small>
                </footer>
              </blockquote>
            </div>
            <div class="card">
              <div class="card-body">
                <h5 class="card-title">Card title</h5>
                <p class="card-text">This is another card with title and supporting text below. This card has some
                  additional content to make it slightly taller overall.</p>
                <p class="card-text"><small class="text-muted">Last updated 3 mins ago</small></p>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;card-columns&quot;&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title that wraps to a new line&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This is a longer card with supporting text below as a natural lead-in to additional content. This content is a little bit longer.&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card p-3&quot;&gt;
        &lt;blockquote class=&quot;blockquote mb-0 card-body&quot;&gt;
          &lt;p&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.&lt;/p&gt;
          &lt;footer class=&quot;blockquote-footer&quot;&gt;
            &lt;small class=&quot;text-muted&quot;&gt;
              Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;
            &lt;/small&gt;
          &lt;/footer&gt;
        &lt;/blockquote&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This card has supporting text below as a natural lead-in to additional content.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card bg-primary text-white text-center p-3&quot;&gt;
        &lt;blockquote class=&quot;blockquote mb-0&quot;&gt;
          &lt;p&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat.&lt;/p&gt;
          &lt;footer class=&quot;blockquote-footer&quot;&gt;
            &lt;small&gt;
              Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;
            &lt;/small&gt;
          &lt;/footer&gt;
        &lt;/blockquote&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card text-center&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This card has a regular title and short paragraphy of text below it.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card p-3 text-right&quot;&gt;
        &lt;blockquote class=&quot;blockquote mb-0&quot;&gt;
          &lt;p&gt;Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer posuere erat a ante.&lt;/p&gt;
          &lt;footer class=&quot;blockquote-footer&quot;&gt;
            &lt;small class=&quot;text-muted&quot;&gt;
              Someone famous in &lt;cite title=&quot;Source Title&quot;&gt;Source Title&lt;/cite&gt;
            &lt;/small&gt;
          &lt;/footer&gt;
        &lt;/blockquote&gt;
      &lt;/div&gt;
      &lt;div class=&quot;card&quot;&gt;
        &lt;div class=&quot;card-body&quot;&gt;
          &lt;h5 class=&quot;card-title&quot;&gt;Card title&lt;/h5&gt;
          &lt;p class=&quot;card-text&quot;&gt;This is another card with title and supporting text below. This card has some additional content to make it slightly taller overall.&lt;/p&gt;
          &lt;p class=&quot;card-text&quot;&gt;&lt;small class=&quot;text-muted&quot;&gt;Last updated 3 mins ago&lt;/small&gt;&lt;/p&gt;
        &lt;/div&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <h1>Buttons</h1>

    <section>
      <h2>Example</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary">Primary</button>
          <button type="button" class="btn btn-secondary">Secondary</button>
          <button type="button" class="btn btn-success">Success</button>
          <button type="button" class="btn btn-danger">Danger</button>
          <button type="button" class="btn btn-warning">Warning</button>
          <button type="button" class="btn btn-info">Info</button>
          <button type="button" class="btn btn-light">Light</button>
          <button type="button" class="btn btn-dark">Dark</button>

          <button type="button" class="btn btn-link">Link</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot;&gt;Primary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Secondary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-success&quot;&gt;Success&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-danger&quot;&gt;Danger&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-warning&quot;&gt;Warning&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-info&quot;&gt;Info&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-light&quot;&gt;Light&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-dark&quot;&gt;Dark&lt;/button&gt;

    &lt;button type=&quot;button&quot; class=&quot;btn btn-link&quot;&gt;Link&lt;/button&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Outline buttons</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-outline-primary">Primary</button>
          <button type="button" class="btn btn-outline-secondary">Secondary</button>
          <button type="button" class="btn btn-outline-success">Success</button>
          <button type="button" class="btn btn-outline-danger">Danger</button>
          <button type="button" class="btn btn-outline-warning">Warning</button>
          <button type="button" class="btn btn-outline-info">Info</button>
          <button type="button" class="btn btn-outline-light">Light</button>
          <button type="button" class="btn btn-outline-dark">Dark</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-primary&quot;&gt;Primary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-secondary&quot;&gt;Secondary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-success&quot;&gt;Success&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-danger&quot;&gt;Danger&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-warning&quot;&gt;Warning&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-info&quot;&gt;Info&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-light&quot;&gt;Light&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-outline-dark&quot;&gt;Dark&lt;/button&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Sizes</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary btn-lg">Primary</button>
          <button type="button" class="btn btn-secondary btn-lg">Secondary</button>
          <button type="button" class="btn btn-success btn-lg">Success</button>
          <button type="button" class="btn btn-danger btn-lg">Danger</button>
          <button type="button" class="btn btn-warning btn-lg">Warning</button>
          <button type="button" class="btn btn-info btn-lg">Info</button>
          <button type="button" class="btn btn-light btn-lg">Light</button>
          <button type="button" class="btn btn-dark btn-lg">Dark</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary btn-lg&quot;&gt;Primary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary btn-lg&quot;&gt;Secondary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-success btn-lg&quot;&gt;Success&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-danger btn-lg&quot;&gt;Danger&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-warning btn-lg&quot;&gt;Warning&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-info btn-lg&quot;&gt;Info&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-light btn-lg&quot;&gt;Light&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-dark btn-lg&quot;&gt;Dark&lt;/button&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary btn-sm">Primary</button>
          <button type="button" class="btn btn-secondary btn-sm">Secondary</button>
          <button type="button" class="btn btn-success btn-sm">Success</button>
          <button type="button" class="btn btn-danger btn-sm">Danger</button>
          <button type="button" class="btn btn-warning btn-sm">Warning</button>
          <button type="button" class="btn btn-info btn-sm">Info</button>
          <button type="button" class="btn btn-light btn-sm">Light</button>
          <button type="button" class="btn btn-dark btn-sm">Dark</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary btn-sm&quot;&gt;Primary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary btn-sm&quot;&gt;Secondary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-success btn-sm&quot;&gt;Success&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-danger btn-sm&quot;&gt;Danger&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-warning btn-sm&quot;&gt;Warning&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-info btn-sm&quot;&gt;Info&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-light btn-sm&quot;&gt;Light&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-dark btn-sm&quot;&gt;Dark&lt;/button&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary btn-lg btn-block">Block level button</button>
          <button type="button" class="btn btn-secondary btn-lg btn-block">Block level button</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary btn-lg btn-block&quot;&gt;Block level button&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary btn-lg btn-block&quot;&gt;Block level button&lt;/button&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Disabled state</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary" disabled>Primary</button>
          <button type="button" class="btn btn-secondary" disabled>Secondary</button>
          <button type="button" class="btn btn-success" disabled>Success</button>
          <button type="button" class="btn btn-danger" disabled>Danger</button>
          <button type="button" class="btn btn-warning" disabled>Warning</button>
          <button type="button" class="btn btn-info" disabled>Info</button>
          <button type="button" class="btn btn-light" disabled>Light</button>
          <button type="button" class="btn btn-dark" disabled>Dark</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; disabled&gt;Primary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot; disabled&gt;Secondary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-success&quot; disabled&gt;Success&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-danger&quot; disabled&gt;Danger&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-warning&quot; disabled&gt;Warning&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-info&quot; disabled&gt;Info&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-light&quot; disabled&gt;Light&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-dark&quot; disabled&gt;Dark&lt;/button&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Active state</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary active">Primary</button>
          <button type="button" class="btn btn-secondary active">Secondary</button>
          <button type="button" class="btn btn-success active">Success</button>
          <button type="button" class="btn btn-danger active">Danger</button>
          <button type="button" class="btn btn-warning active">Warning</button>
          <button type="button" class="btn btn-info active">Info</button>
          <button type="button" class="btn btn-light active">Light</button>
          <button type="button" class="btn btn-dark active">Dark</button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary active&quot;&gt;Primary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary active&quot;&gt;Secondary&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-success active&quot;&gt;Success&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-danger active&quot;&gt;Danger&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-warning active&quot;&gt;Warning&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-info active&quot;&gt;Info&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-light active&quot;&gt;Light&lt;/button&gt;
    &lt;button type=&quot;button&quot; class=&quot;btn btn-dark active&quot;&gt;Dark&lt;/button&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Button tags</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <a class="btn btn-primary" href="#" role="button">Link</a>
          <button class="btn btn-primary" type="submit">Button</button>
          <input class="btn btn-primary" type="button" value="Input">
          <input class="btn btn-primary" type="submit" value="Submit">
          <input class="btn btn-primary" type="reset" value="Reset">
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;a class=&quot;btn btn-primary&quot; href=&quot;#&quot; role=&quot;button&quot;&gt;Link&lt;/a&gt;
    &lt;button class=&quot;btn btn-primary&quot; type=&quot;submit&quot;&gt;Button&lt;/button&gt;
    &lt;input class=&quot;btn btn-primary&quot; type=&quot;button&quot; value=&quot;Input&quot;&gt;
    &lt;input class=&quot;btn btn-primary&quot; type=&quot;submit&quot; value=&quot;Submit&quot;&gt;
    &lt;input class=&quot;btn btn-primary&quot; type=&quot;reset&quot; value=&quot;Reset&quot;&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Button plugin</h2>
      <h3>Toggle states</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <button type="button" class="btn btn-primary" data-toggle="button" aria-pressed="false" autocomplete="off">
            Single toggle
          </button>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;button type=&quot;button&quot; class=&quot;btn btn-primary&quot; data-toggle=&quot;button&quot; aria-pressed=&quot;false&quot; autocomplete=&quot;off&quot;&gt;
      Single toggle
    &lt;/button&gt;
    </code>
  </pre>

      <h3>Checkbox and radio buttons</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group-toggle" data-toggle="buttons">
            <label class="btn btn-secondary active">
              <input type="checkbox" checked autocomplete="off"> Checked
            </label>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-group-toggle&quot; data-toggle=&quot;buttons&quot;&gt;
      &lt;label class=&quot;btn btn-secondary active&quot;&gt;
        &lt;input type=&quot;checkbox&quot; checked autocomplete=&quot;off&quot;&gt; Checked
      &lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <div class="btn-group btn-group-toggle" data-toggle="buttons">
            <label class="btn btn-secondary active">
              <input type="radio" name="options" id="option1" autocomplete="off" checked> Active
            </label>
            <label class="btn btn-secondary">
              <input type="radio" name="options" id="option2" autocomplete="off"> Radio
            </label>
            <label class="btn btn-secondary">
              <input type="radio" name="options" id="option3" autocomplete="off"> Radio
            </label>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-group btn-group-toggle&quot; data-toggle=&quot;buttons&quot;&gt;
      &lt;label class=&quot;btn btn-secondary active&quot;&gt;
        &lt;input type=&quot;radio&quot; name=&quot;options&quot; id=&quot;option1&quot; autocomplete=&quot;off&quot; checked&gt; Active
      &lt;/label&gt;
      &lt;label class=&quot;btn btn-secondary&quot;&gt;
        &lt;input type=&quot;radio&quot; name=&quot;options&quot; id=&quot;option2&quot; autocomplete=&quot;off&quot;&gt; Radio
      &lt;/label&gt;
      &lt;label class=&quot;btn btn-secondary&quot;&gt;
        &lt;input type=&quot;radio&quot; name=&quot;options&quot; id=&quot;option3&quot; autocomplete=&quot;off&quot;&gt; Radio
      &lt;/label&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Button groups</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group" role="group" aria-label="Basic example">
            <button type="button" class="btn btn-secondary">Left</button>
            <button type="button" class="btn btn-secondary">Middle</button>
            <button type="button" class="btn btn-secondary">Right</button>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-group&quot; role=&quot;group&quot; aria-label=&quot;Basic example&quot;&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Left&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Middle&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Right&lt;/button&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Button toolbar</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-toolbar" role="toolbar" aria-label="Toolbar with button groups">
            <div class="btn-group mr-2" role="group" aria-label="First group">
              <button type="button" class="btn btn-secondary">1</button>
              <button type="button" class="btn btn-secondary">2</button>
              <button type="button" class="btn btn-secondary">3</button>
              <button type="button" class="btn btn-secondary">4</button>
            </div>
            <div class="btn-group mr-2" role="group" aria-label="Second group">
              <button type="button" class="btn btn-secondary">5</button>
              <button type="button" class="btn btn-secondary">6</button>
              <button type="button" class="btn btn-secondary">7</button>
            </div>
            <div class="btn-group" role="group" aria-label="Third group">
              <button type="button" class="btn btn-secondary">8</button>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-toolbar&quot; role=&quot;toolbar&quot; aria-label=&quot;Toolbar with button groups&quot;&gt;
      &lt;div class=&quot;btn-group mr-2&quot; role=&quot;group&quot; aria-label=&quot;First group&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;1&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;2&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;3&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;4&lt;/button&gt;
      &lt;/div&gt;
      &lt;div class=&quot;btn-group mr-2&quot; role=&quot;group&quot; aria-label=&quot;Second group&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;5&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;6&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;7&lt;/button&gt;
      &lt;/div&gt;
      &lt;div class=&quot;btn-group&quot; role=&quot;group&quot; aria-label=&quot;Third group&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;8&lt;/button&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <div class="row">
        <div class="col-12">
          <div class="btn-toolbar mb-3" role="toolbar" aria-label="Toolbar with button groups">
            <div class="btn-group mr-2" role="group" aria-label="First group">
              <button type="button" class="btn btn-secondary">1</button>
              <button type="button" class="btn btn-secondary">2</button>
              <button type="button" class="btn btn-secondary">3</button>
              <button type="button" class="btn btn-secondary">4</button>
            </div>
            <div class="input-group">
              <div class="input-group-prepend">
                <div class="input-group-text" id="btnGroupAddon">@</div>
              </div>
              <input type="text" class="form-control" placeholder="Input group example" aria-label="Input group example"
                aria-describedby="btnGroupAddon">
            </div>
          </div>

          <div class="btn-toolbar justify-content-between" role="toolbar" aria-label="Toolbar with button groups">
            <div class="btn-group" role="group" aria-label="First group">
              <button type="button" class="btn btn-secondary">1</button>
              <button type="button" class="btn btn-secondary">2</button>
              <button type="button" class="btn btn-secondary">3</button>
              <button type="button" class="btn btn-secondary">4</button>
            </div>
            <div class="input-group">
              <div class="input-group-prepend">
                <div class="input-group-text" id="btnGroupAddon2">@</div>
              </div>
              <input type="text" class="form-control" placeholder="Input group example" aria-label="Input group example"
                aria-describedby="btnGroupAddon2">
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-toolbar mb-3&quot; role=&quot;toolbar&quot; aria-label=&quot;Toolbar with button groups&quot;&gt;
      &lt;div class=&quot;btn-group mr-2&quot; role=&quot;group&quot; aria-label=&quot;First group&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;1&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;2&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;3&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;4&lt;/button&gt;
      &lt;/div&gt;
      &lt;div class=&quot;input-group&quot;&gt;
        &lt;div class=&quot;input-group-prepend&quot;&gt;
          &lt;div class=&quot;input-group-text&quot; id=&quot;btnGroupAddon&quot;&gt;@&lt;/div&gt;
        &lt;/div&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; placeholder=&quot;Input group example&quot; aria-label=&quot;Input group example&quot; aria-describedby=&quot;btnGroupAddon&quot;&gt;
      &lt;/div&gt;
    &lt;/div&gt;

    &lt;div class=&quot;btn-toolbar justify-content-between&quot; role=&quot;toolbar&quot; aria-label=&quot;Toolbar with button groups&quot;&gt;
      &lt;div class=&quot;btn-group&quot; role=&quot;group&quot; aria-label=&quot;First group&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;1&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;2&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;3&lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;4&lt;/button&gt;
      &lt;/div&gt;
      &lt;div class=&quot;input-group&quot;&gt;
        &lt;div class=&quot;input-group-prepend&quot;&gt;
          &lt;div class=&quot;input-group-text&quot; id=&quot;btnGroupAddon2&quot;&gt;@&lt;/div&gt;
        &lt;/div&gt;
        &lt;input type=&quot;text&quot; class=&quot;form-control&quot; placeholder=&quot;Input group example&quot; aria-label=&quot;Input group example&quot; aria-describedby=&quot;btnGroupAddon2&quot;&gt;
      &lt;/div&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Sizing</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group btn-group-lg" role="group">
            <button type="button" class="btn btn-secondary">Left</button>
            <button type="button" class="btn btn-secondary">Middle</button>
            <button type="button" class="btn btn-secondary">Right</button>
          </div>
          <div class="btn-group btn-group" role="group">
            <button type="button" class="btn btn-secondary">Left</button>
            <button type="button" class="btn btn-secondary">Middle</button>
            <button type="button" class="btn btn-secondary">Right</button>
          </div>
          <div class="btn-group btn-group-sm" role="group">
            <button type="button" class="btn btn-secondary">Left</button>
            <button type="button" class="btn btn-secondary">Middle</button>
            <button type="button" class="btn btn-secondary">Right</button>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-group btn-group-lg&quot; role=&quot;group&quot;&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Left&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Middle&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Right&lt;/button&gt;
    &lt;/div&gt;
    &lt;div class=&quot;btn-group btn-group&quot; role=&quot;group&quot;&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Left&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Middle&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Right&lt;/button&gt;
    &lt;/div&gt;
    &lt;div class=&quot;btn-group btn-group-sm&quot; role=&quot;group&quot;&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Left&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Middle&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Right&lt;/button&gt;
    &lt;/div&gt;
    </code>
  </pre>

      <h3>Vertical variation</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group-vertical" role="group" aria-label="Vertical button group">
            <button type="button" class="btn btn-secondary">Button</button>
            <button type="button" class="btn btn-secondary">Button</button>
            <button type="button" class="btn btn-secondary">Button</button>
            <button type="button" class="btn btn-secondary">Button</button>
            <button type="button" class="btn btn-secondary">Button</button>
            <button type="button" class="btn btn-secondary">Button</button>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
    <code class="collapse html">
    &lt;div class=&quot;btn-group-vertical&quot; role=&quot;group&quot; aria-label=&quot;Vertical button group&quot;&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Button&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Button&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Button&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Button&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Button&lt;/button&gt;
      &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;Button&lt;/button&gt;
    &lt;/div&gt;
    </code>
  </pre>
    </section>

    <section>
      <h2>Dropdowns</h2>
      <h3>Single button</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="dropdown mb-2">
            <button class="btn btn-secondary dropdown-toggle" type="button" id="dropdownMenuButton" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              Dropdown button
            </button>
            <div class="dropdown-menu" aria-labelledby="dropdownMenuButton">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
            </div>
          </div>
          <div class="btn-group">
            <button type="button" class="btn btn-danger dropdown-toggle" data-toggle="dropdown" aria-haspopup="true"
              aria-expanded="false">
              Action
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
        <code class="collapse html">
        &lt;div class=&quot;dropdown&quot;&gt;
          &lt;button class=&quot;btn btn-secondary dropdown-toggle&quot; type=&quot;button&quot; id=&quot;dropdownMenuButton&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            Dropdown button
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot; aria-labelledby=&quot;dropdownMenuButton&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;

        &lt;!-- Example single danger button --&gt;
        &lt;div class=&quot;btn-group&quot;&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-danger dropdown-toggle&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            Action
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        </code>
      </pre>

      <h3>Split button</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <!-- Example split danger button -->
          <div class="btn-group">
            <button type="button" class="btn btn-primary">Primary</button>
            <button type="button" class="btn btn-primary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
          <div class="btn-group">
            <button type="button" class="btn btn-secondary">Secondary</button>
            <button type="button" class="btn btn-secondary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
          <div class="btn-group">
            <button type="button" class="btn btn-success">Success</button>
            <button type="button" class="btn btn-success dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
          <div class="btn-group">
            <button type="button" class="btn btn-info">Info</button>
            <button type="button" class="btn btn-info dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
          <div class="btn-group">
            <button type="button" class="btn btn-warning">Warning</button>
            <button type="button" class="btn btn-warning dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
          <div class="btn-group">
            <button type="button" class="btn btn-danger">Danger</button>
            <button type="button" class="btn btn-danger dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
        <code class="collapse html">
        &lt;!-- Example split danger button --&gt;
        &lt;div class=&quot;btn-group&quot;&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-danger&quot;&gt;Action&lt;/button&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-danger dropdown-toggle dropdown-toggle-split&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            &lt;span class=&quot;sr-only&quot;&gt;Toggle Dropdown&lt;/span&gt;
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        </code>
      </pre>
    </section>

    <section>
      <h2>Sizing</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-toolbar mb-2" role="toolbar">
            <div class="btn-group">
              <button class="btn btn-secondary btn-lg dropdown-toggle" type="button" data-toggle="dropdown"
                aria-haspopup="true" aria-expanded="false">
                Large button
              </button>
              <div class="dropdown-menu" x-placement="bottom-start">
                <a class="dropdown-item" href="#">Action</a>
                <a class="dropdown-item" href="#">Another action</a>
                <a class="dropdown-item" href="#">Something else here</a>
                <div class="dropdown-divider"></div>
                <a class="dropdown-item" href="#">Separated link</a>
              </div>
            </div>
            <div class="btn-group ml-2">
              <button type="button" class="btn btn-lg btn-secondary">Large split button</button>
              <button type="button" class="btn btn-lg btn-secondary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
                aria-haspopup="true" aria-expanded="false">
                <span class="sr-only">Toggle Dropdown</span>
              </button>
              <div class="dropdown-menu" x-placement="bottom-start">
                <a class="dropdown-item" href="#">Action</a>
                <a class="dropdown-item" href="#">Another action</a>
                <a class="dropdown-item" href="#">Something else here</a>
                <div class="dropdown-divider"></div>
                <a class="dropdown-item" href="#">Separated link</a>
              </div>
            </div>
          </div>
          <div class="btn-toolbar" role="toolbar">
            <div class="btn-group">
              <button class="btn btn-secondary btn-sm dropdown-toggle" type="button" data-toggle="dropdown"
                aria-haspopup="true" aria-expanded="false">
                Small button
              </button>
              <div class="dropdown-menu" x-placement="bottom-start">
                <a class="dropdown-item" href="#">Action</a>
                <a class="dropdown-item" href="#">Another action</a>
                <a class="dropdown-item" href="#">Something else here</a>
                <div class="dropdown-divider"></div>
                <a class="dropdown-item" href="#">Separated link</a>
              </div>
            </div>
            <div class="btn-group ml-2">
              <button type="button" class="btn btn-sm btn-secondary">Small split button</button>
              <button type="button" class="btn btn-sm btn-secondary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
                aria-haspopup="true" aria-expanded="false">
                <span class="sr-only">Toggle Dropdown</span>
              </button>
              <div class="dropdown-menu" x-placement="bottom-start">
                <a class="dropdown-item" href="#">Action</a>
                <a class="dropdown-item" href="#">Another action</a>
                <a class="dropdown-item" href="#">Something else here</a>
                <div class="dropdown-divider"></div>
                <a class="dropdown-item" href="#">Separated link</a>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
      &lt;div class=&quot;btn-toolbar mb-2&quot; role=&quot;toolbar&quot;&gt;
        &lt;div class=&quot;btn-group&quot;&gt;
          &lt;button class=&quot;btn btn-secondary btn-lg dropdown-toggle&quot; type=&quot;button&quot; data-toggle=&quot;dropdown&quot;
            aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            Large button
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot; x-placement=&quot;bottom-start&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;btn-group ml-2&quot;&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-lg btn-secondary&quot;&gt;Large split button&lt;/button&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-lg btn-secondary dropdown-toggle dropdown-toggle-split&quot; data-toggle=&quot;dropdown&quot;
            aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            &lt;span class=&quot;sr-only&quot;&gt;Toggle Dropdown&lt;/span&gt;
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot; x-placement=&quot;bottom-start&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      &lt;div class=&quot;btn-toolbar&quot; role=&quot;toolbar&quot;&gt;
        &lt;div class=&quot;btn-group&quot;&gt;
          &lt;button class=&quot;btn btn-secondary btn-sm dropdown-toggle&quot; type=&quot;button&quot; data-toggle=&quot;dropdown&quot;
            aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            Small button
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot; x-placement=&quot;bottom-start&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;btn-group ml-2&quot;&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-sm btn-secondary&quot;&gt;Small split button&lt;/button&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-sm btn-secondary dropdown-toggle dropdown-toggle-split&quot; data-toggle=&quot;dropdown&quot;
            aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            &lt;span class=&quot;sr-only&quot;&gt;Toggle Dropdown&lt;/span&gt;
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot; x-placement=&quot;bottom-start&quot;&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Another action&lt;/a&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Something else here&lt;/a&gt;
            &lt;div class=&quot;dropdown-divider&quot;&gt;&lt;/div&gt;
            &lt;a class=&quot;dropdown-item&quot; href=&quot;#&quot;&gt;Separated link&lt;/a&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      </code>
    </pre>
    </section>

    <section>
      <h2>Directions</h2>
      <h3>Dropup</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group dropup">
            <button type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true"
              aria-expanded="false">
              Dropup
            </button>
            <div class="dropdown-menu" x-placement="top-start">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>

          <div class="btn-group dropup">
            <button type="button" class="btn btn-secondary">
              Split dropup
            </button>
            <button type="button" class="btn btn-secondary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
      &lt;!-- Default dropup button --&gt;
      &lt;div class=&quot;btn-group dropup&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary dropdown-toggle&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
          Dropup
        &lt;/button&gt;
        &lt;div class=&quot;dropdown-menu&quot;&gt;
          &lt;!-- Dropdown menu links --&gt;
        &lt;/div&gt;
      &lt;/div&gt;

      &lt;!-- Split dropup button --&gt;
      &lt;div class=&quot;btn-group dropup&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;
          Split dropup
        &lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary dropdown-toggle dropdown-toggle-split&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
          &lt;span class=&quot;sr-only&quot;&gt;Toggle Dropdown&lt;/span&gt;
        &lt;/button&gt;
        &lt;div class=&quot;dropdown-menu&quot;&gt;
          &lt;!-- Dropdown menu links --&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      </code>
    </pre>

      <h3>Dropright</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group dropright">
            <button type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true"
              aria-expanded="false">
              Dropright
            </button>
            <div class="dropdown-menu" x-placement="top-start">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>

          <div class="btn-group dropright">
            <button type="button" class="btn btn-secondary">
              Split dropright
            </button>
            <button type="button" class="btn btn-secondary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
              aria-haspopup="true" aria-expanded="false">
              <span class="sr-only">Toggle Dropdown</span>
            </button>
            <div class="dropdown-menu">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
      &lt;!-- Default dropup button --&gt;
      &lt;div class=&quot;btn-group dropright&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary dropdown-toggle&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
          Dropright
        &lt;/button&gt;
        &lt;div class=&quot;dropdown-menu&quot;&gt;
          &lt;!-- Dropdown menu links --&gt;
        &lt;/div&gt;
      &lt;/div&gt;

      &lt;!-- Split dropup button --&gt;
      &lt;div class=&quot;btn-group dropright&quot;&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;
          Split dropright
        &lt;/button&gt;
        &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary dropdown-toggle dropdown-toggle-split&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
          &lt;span class=&quot;sr-only&quot;&gt;Toggle Dropdown&lt;/span&gt;
        &lt;/button&gt;
        &lt;div class=&quot;dropdown-menu&quot;&gt;
          &lt;!-- Dropdown menu links --&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      </code>
    </pre>

      <h3>Dropleft</h3>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="btn-group dropleft">
            <button type="button" class="btn btn-secondary dropdown-toggle" data-toggle="dropdown" aria-haspopup="true"
              aria-expanded="false">
              Dropleft
            </button>
            <div class="dropdown-menu" x-placement="top-start">
              <a class="dropdown-item" href="#">Action</a>
              <a class="dropdown-item" href="#">Another action</a>
              <a class="dropdown-item" href="#">Something else here</a>
              <div class="dropdown-divider"></div>
              <a class="dropdown-item" href="#">Separated link</a>
            </div>
          </div>

          <div class="btn-group">
            <div class="btn-group dropleft" role="group">
              <button type="button" class="btn btn-secondary dropdown-toggle dropdown-toggle-split" data-toggle="dropdown"
                aria-haspopup="true" aria-expanded="false">
                <span class="sr-only">Toggle Dropleft</span>
              </button>
              <div class="dropdown-menu">
                <a class="dropdown-item" href="#">Action</a>
                <a class="dropdown-item" href="#">Another action</a>
                <a class="dropdown-item" href="#">Something else here</a>
                <div class="dropdown-divider"></div>
                <a class="dropdown-item" href="#">Separated link</a>
              </div>
            </div>
            <button type="button" class="btn btn-secondary">
              Split dropleft
            </button>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
        &lt;!-- Default dropleft button --&gt;
        &lt;div class=&quot;btn-group dropleft&quot;&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary dropdown-toggle&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
            Dropleft
          &lt;/button&gt;
          &lt;div class=&quot;dropdown-menu&quot;&gt;
            &lt;!-- Dropdown menu links --&gt;
          &lt;/div&gt;
        &lt;/div&gt;

        &lt;!-- Split dropleft button --&gt;
        &lt;div class=&quot;btn-group&quot;&gt;
          &lt;div class=&quot;btn-group dropleft&quot; role=&quot;group&quot;&gt;
            &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary dropdown-toggle dropdown-toggle-split&quot; data-toggle=&quot;dropdown&quot; aria-haspopup=&quot;true&quot; aria-expanded=&quot;false&quot;&gt;
              &lt;span class=&quot;sr-only&quot;&gt;Toggle Dropleft&lt;/span&gt;
            &lt;/button&gt;
            &lt;div class=&quot;dropdown-menu&quot;&gt;
              &lt;!-- Dropdown menu links --&gt;
            &lt;/div&gt;
          &lt;/div&gt;
          &lt;button type=&quot;button&quot; class=&quot;btn btn-secondary&quot;&gt;
            Split dropleft
          &lt;/button&gt;
        &lt;/div&gt;
      </code>
    </pre>
    </section>

    <h1>Collapse</h1>

    <section>
      <h2>Example</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <p>
            <a class="btn btn-primary" data-toggle="collapse" href="#collapseExample" role="button" aria-expanded="false"
              aria-controls="collapseExample">
              Link with href
            </a>
            <button class="btn btn-primary" type="button" data-toggle="collapse" data-target="#collapseExample"
              aria-expanded="false" aria-controls="collapseExample">
              Button with data-target
            </button>
          </p>
          <div class="collapse" id="collapseExample">
            <div class="card card-body">
              Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. Nihil
              anim
              keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
      &lt;p&gt;
        &lt;a class=&quot;btn btn-primary&quot; data-toggle=&quot;collapse&quot; href=&quot;#collapseExample&quot; role=&quot;button&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;collapseExample&quot;&gt;
          Link with href
        &lt;/a&gt;
        &lt;button class=&quot;btn btn-primary&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#collapseExample&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;collapseExample&quot;&gt;
          Button with data-target
        &lt;/button&gt;
      &lt;/p&gt;
      &lt;div class=&quot;collapse&quot; id=&quot;collapseExample&quot;&gt;
        &lt;div class=&quot;card card-body&quot;&gt;
          Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
        &lt;/div&gt;
      &lt;/div&gt;
      </code>
    </pre>
    </section>

    <section>
      <h2>Multiple targets</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <p>
            <a class="btn btn-primary" data-toggle="collapse" href="#multiCollapseExample1" role="button" aria-expanded="false"
              aria-controls="multiCollapseExample1">Toggle first element</a>
            <button class="btn btn-primary" type="button" data-toggle="collapse" data-target="#multiCollapseExample2"
              aria-expanded="false" aria-controls="multiCollapseExample2">Toggle second element</button>
            <button class="btn btn-primary" type="button" data-toggle="collapse" data-target=".multi-collapse"
              aria-expanded="false" aria-controls="multiCollapseExample1 multiCollapseExample2">Toggle both elements</button>
          </p>
          <div class="row">
            <div class="col">
              <div class="collapse multi-collapse" id="multiCollapseExample1">
                <div class="card card-body">
                  Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. Nihil
                  anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
                </div>
              </div>
            </div>
            <div class="col">
              <div class="collapse multi-collapse" id="multiCollapseExample2">
                <div class="card card-body">
                  Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. Nihil
                  anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
      &lt;p&gt;
        &lt;a class=&quot;btn btn-primary&quot; data-toggle=&quot;collapse&quot; href=&quot;#multiCollapseExample1&quot; role=&quot;button&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;multiCollapseExample1&quot;&gt;Toggle first element&lt;/a&gt;
        &lt;button class=&quot;btn btn-primary&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#multiCollapseExample2&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;multiCollapseExample2&quot;&gt;Toggle second element&lt;/button&gt;
        &lt;button class=&quot;btn btn-primary&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;.multi-collapse&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;multiCollapseExample1 multiCollapseExample2&quot;&gt;Toggle both elements&lt;/button&gt;
      &lt;/p&gt;
      &lt;div class=&quot;row&quot;&gt;
        &lt;div class=&quot;col&quot;&gt;
          &lt;div class=&quot;collapse multi-collapse&quot; id=&quot;multiCollapseExample1&quot;&gt;
            &lt;div class=&quot;card card-body&quot;&gt;
              Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;col&quot;&gt;
          &lt;div class=&quot;collapse multi-collapse&quot; id=&quot;multiCollapseExample2&quot;&gt;
            &lt;div class=&quot;card card-body&quot;&gt;
              Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      </code>
    </pre>
    </section>

    <section>
      <h2>Accordion example</h2>
      <hr>
      <div class="row">
        <div class="col-12">
          <div class="accordion" id="accordionExample">
            <div class="card">
              <div class="card-header" id="headingOne">
                <h5 class="mb-0">
                  <button class="btn btn-link" type="button" data-toggle="collapse" data-target="#collapseOne"
                    aria-expanded="true" aria-controls="collapseOne">
                    Collapsible Group Item #1
                  </button>
                </h5>
              </div>

              <div id="collapseOne" class="collapse show" aria-labelledby="headingOne" data-parent="#accordionExample">
                <div class="card-body">
                  Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3
                  wolf
                  moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod.
                  Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda
                  shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente
                  ea
                  proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim
                  aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
                </div>
              </div>
            </div>
            <div class="card">
              <div class="card-header" id="headingTwo">
                <h5 class="mb-0">
                  <button class="btn btn-link collapsed" type="button" data-toggle="collapse" data-target="#collapseTwo"
                    aria-expanded="false" aria-controls="collapseTwo">
                    Collapsible Group Item #2
                  </button>
                </h5>
              </div>
              <div id="collapseTwo" class="collapse" aria-labelledby="headingTwo" data-parent="#accordionExample">
                <div class="card-body">
                  Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3
                  wolf
                  moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod.
                  Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda
                  shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente
                  ea
                  proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim
                  aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
                </div>
              </div>
            </div>
            <div class="card">
              <div class="card-header" id="headingThree">
                <h5 class="mb-0">
                  <button class="btn btn-link collapsed" type="button" data-toggle="collapse" data-target="#collapseThree"
                    aria-expanded="false" aria-controls="collapseThree">
                    Collapsible Group Item #3
                  </button>
                </h5>
              </div>
              <div id="collapseThree" class="collapse" aria-labelledby="headingThree" data-parent="#accordionExample">
                <div class="card-body">
                  Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3
                  wolf
                  moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod.
                  Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda
                  shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente
                  ea
                  proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim
                  aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <pre class="m-0 col-12">
      <code class="collapse html">
      &lt;div class=&quot;accordion&quot; id=&quot;accordionExample&quot;&gt;
        &lt;div class=&quot;card&quot;&gt;
          &lt;div class=&quot;card-header&quot; id=&quot;headingOne&quot;&gt;
            &lt;h5 class=&quot;mb-0&quot;&gt;
              &lt;button class=&quot;btn btn-link&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#collapseOne&quot; aria-expanded=&quot;true&quot; aria-controls=&quot;collapseOne&quot;&gt;
                Collapsible Group Item #1
              &lt;/button&gt;
            &lt;/h5&gt;
          &lt;/div&gt;

          &lt;div id=&quot;collapseOne&quot; class=&quot;collapse show&quot; aria-labelledby=&quot;headingOne&quot; data-parent=&quot;#accordionExample&quot;&gt;
            &lt;div class=&quot;card-body&quot;&gt;
              Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod. Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;card&quot;&gt;
          &lt;div class=&quot;card-header&quot; id=&quot;headingTwo&quot;&gt;
            &lt;h5 class=&quot;mb-0&quot;&gt;
              &lt;button class=&quot;btn btn-link collapsed&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#collapseTwo&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;collapseTwo&quot;&gt;
                Collapsible Group Item #2
              &lt;/button&gt;
            &lt;/h5&gt;
          &lt;/div&gt;
          &lt;div id=&quot;collapseTwo&quot; class=&quot;collapse&quot; aria-labelledby=&quot;headingTwo&quot; data-parent=&quot;#accordionExample&quot;&gt;
            &lt;div class=&quot;card-body&quot;&gt;
              Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod. Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
        &lt;div class=&quot;card&quot;&gt;
          &lt;div class=&quot;card-header&quot; id=&quot;headingThree&quot;&gt;
            &lt;h5 class=&quot;mb-0&quot;&gt;
              &lt;button class=&quot;btn btn-link collapsed&quot; type=&quot;button&quot; data-toggle=&quot;collapse&quot; data-target=&quot;#collapseThree&quot; aria-expanded=&quot;false&quot; aria-controls=&quot;collapseThree&quot;&gt;
                Collapsible Group Item #3
              &lt;/button&gt;
            &lt;/h5&gt;
          &lt;/div&gt;
          &lt;div id=&quot;collapseThree&quot; class=&quot;collapse&quot; aria-labelledby=&quot;headingThree&quot; data-parent=&quot;#accordionExample&quot;&gt;
            &lt;div class=&quot;card-body&quot;&gt;
              Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid. 3 wolf moon officia aute, non cupidatat skateboard dolor brunch. Food truck quinoa nesciunt laborum eiusmod. Brunch 3 wolf moon tempor, sunt aliqua put a bird on it squid single-origin coffee nulla assumenda shoreditch et. Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident. Ad vegan excepteur butcher vice lomo. Leggings occaecat craft beer farm-to-table, raw denim aesthetic synth nesciunt you probably haven't heard of them accusamus labore sustainable VHS.
            &lt;/div&gt;
          &lt;/div&gt;
        &lt;/div&gt;
      &lt;/div&gt;
      </code>
    </pre>
    </section>