<%@ page pageEncoding="utf-8" trimDirectiveWhitespaces="true" %>

<section class="md-product-list list-type-slider">
    <div class="row">
        <div class="col-12 col-md-9">
            <h2>Najnovšie produkty</h2>
        </div>
        <div class="col-12 col-md-3 text-right">
            <button class="btn btn-primary btn-sm">Zobraziť viac</button>
        </div>
    </div>
    <div class="slider">
        #foreach($doc in $news)

        <div class="col">
            <div class="card">
                <div class="card-header">
                    <a href="#">#doc.title</a>
                    <div class="badges">
                        <span class="badge badge-danger">Akcia</span>
                        <span class="badge badge-warning">Top</span>
                        <span class="badge badge-primary">Novinka</span>
                        <span class="badge badge-success">+ Darček</span>
                    </div>
                </div>
                <div class="card-body">
                    <small>$doc.perexPre</small>
                    <div class="alert alert-warning p-1 text-center">
                        Posledný kus za túto cenu
                    </div>
                    <a href="#"><img src="${ninja.temp.basePathImg}iphonex_spacegray.png" alt="product image"></a>
                    <small class="text-success">5ks na sklade</small>
                </div>
                <div class="card-footer">
                    <div class="row">
                        <div class="col-6">
                            <div class="info">
                                <small>ušetríš 200€</small>
                            </div>
                            <span class="price">$doc.fieldK</span>
                        </div>
                        <div class="col-6">
                            <div class="btn-group btn-group-sm">
                                <button type="button" class="btn btn-success" data-bs-toggle="tooltip" title="Pridať do košíka"><i class="fas fa-shopping-basket"></i></button>
                                <button type="button" class="btn btn-success">Kúpiť</button>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        #end
    </div>
</section>