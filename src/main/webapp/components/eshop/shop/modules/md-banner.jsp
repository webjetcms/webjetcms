<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>
<section class="md-banner">
    <div id="banner" class="carousel slide" data-ride="carousel">
        <div class="carousel-inner">
            #foreach($doc in $news)
            <div #if($foreach.first) class="carousel-item active" #else class="carousel-item" #end>
                <img src="/thumb$doc.perexImage?w=1110&h=413&ip=5" alt="image" class="d-block w-100">
                <div class="carousel-caption d-none d-md-block">
                    <div class="row">
                        <div class="col-12 col-md-7 col-lg-6 align-self-center">
                            <h2><a href="$context.link($doc)">$doc.title</a></h2>
                            <p>
                                $doc.perexPre
                            </p>
                            <a href="$context.link($doc)" class="btn btn-secondary">Zistiť viac</a>
                        </div>
                    </div>
                </div>
            </div>
            #end
        </div>
        <a class="carousel-control-prev" href="#banner" role="button" data-slide="prev">
            <i class="fas fa-angle-left"></i>
            <span class="sr-only">Previous</span>
        </a>
        <a class="carousel-control-next" href="#banner" role="button" data-slide="next">
            <i class="fas fa-angle-right"></i>
            <span class="sr-only">Next</span>
        </a>
        <ol class="carousel-indicators">
            #foreach($doc in $news)
            <li data-target="#banner" data-slide-to="$foreach.index" #if($foreach.first) class="active" #end>$doc.title</li>
            #end
        </ol>
    </div>
</section>