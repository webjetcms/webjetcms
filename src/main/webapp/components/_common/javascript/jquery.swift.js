
// jQuery - Swift - Copyright TJ Holowaychuk <tj@vision-media.ca> (MIT Licensed)

;(function($){
  
  // --- Swift instances
  
  var instances = []
  
  /**
   * Generate input buttons from the given _hash_.
   *
   * @param  {object} hash
   * @return {string}
   * @api private
   */
  
  function buttons(hash) {
    var buf = ''
    if (!hash) throw 'swift must be passed a hash of buttons'
    for (var key in hash)
      buf += '<input type="button" name="' + key + '" value="' + hash[key].label + '" />'
    return buf
  }
  
  /**
   * Generate a swift instance with the given _label_ and _options_.
   *
   * Options:
   *
   *  - body:    arbitrary text or markup placed before buttons
   *  - buttons: hash of buttons passed to buttons()
   *
   * @param  {string} label
   * @param  {options} hash
   * @return {jQuery}
   * @api private
   */
  
  function template(label, options) {
    options = options || {}
    var instance = $('<span class="swift east">       \
      <span class="wrapper">                          \
        <span class="title">' + label + '</span>      \
        <span class="body">' + (options.body || '') + 
        buttons(options.buttons) + '</span> \
      </span>                                         \
    </span>')
    instances.push(instance)
    return instance
  }
  
  /**
   * Close all swift instances with the given _options_.
   *
   * Options:
   *
   *  - duration:  animation duration in milliseconds
   *
   * @param  {object} options
   * @api private
   */
  
  function close(options) {
    var instance
    options = options || {}
    while (instance = instances.pop())
      instance.animate({
        left: instance.offset().left - 50,
        opacity: 0
      }, options.duration || 500, function(){
        $(this).remove()
      })
  }
  
  /**
   * Create a swift instance applied to the first element
   * in the collection. Self-positions based on it's offset.
   *
   * Returns the collection of swift elements.
   *
   * Options:
   *
   *  - duration:  animation duration in milliseconds 
   *  - body:      arbitrary text or markup placed before buttons
   *  - buttons:   hash of buttons passed to buttons()
   *
   * @param  {string} label
   * @param  {object} options
   * @return {jQuery}
   * @api public
   */
  
  $.fn.swift = function(label, options) {
    close(options = options || {})
    var self, top = this.offset().top, target = this, left = this.offset().left + this.width()
    return (self = template(label, options)).appendTo('body').css({
      position: 'absolute',
      top: top,
      left: left - 250,
      opacity: 0
    })
    .animate({
      top: top - 30,
      opacity: 1
    }, options.duration || 500)
    .find(':button')
    .click(function(e){
      e.target = target
      var name = $(this).attr('name')
      if (name in options.buttons)
        if (options.buttons[name].action.call(self, e) !== false)
          close()
    })
    .end()
  }
  
})(jQuery)