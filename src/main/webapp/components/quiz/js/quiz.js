function Quiz(qId) {
	var questionBoxes = $('#quizForm .qBox');
	var steps = questionBoxes.length;
	var actualStep = 0;
	var formId = null;
	var multiStep = true;

	this.setMultiStep = function(val) {
		multiStep = val;
		this.showActualStep();
	}
	
	this.getMultiStep = function() {
	    return multiStep;
    }

	this.setQuizId = function(qqId) {
		quizId = qqId;
	};
	
	this.getQuizId = function() {
		return quizId;
	};
	
	this.getActualStep = function() {
		return actualStep;
	};
	
	this.getAnswers = function() {
		return $('#quizForm').serializeArray();
	};
	
	this.getAnswersJSONString = function() {
		return JSON.stringify( $('#quizForm').serializeArray() );
	};
	
	this.generateFormId = function() {
		formId = $.sha1((navigator.userAgent + new Date().getTime()));
	};
	
	this.getFormId = function() {
		return formId;
	};
	
	this.getActualStepOfTotal = function() {
		return (actualStep + 1) + "/" + steps;
	};
	
	this.getActualProgress = function() {
		return Math.round((actualStep + 1) / steps * 100);
	};
	
	this.showActualStep = function() {
		// show step
        $('#correctAnswers').hide();
		if(multiStep) {
            $('.btn.sendResults').hide();
            questionBoxes.hide();
            $('#qBox' + actualStep).show();

            // show progress
            $('.progressNumber').text(this.getActualStepOfTotal());
            $('.progressBar .progress-bar').css('width', this.getActualProgress() + '%').attr('aria-valuenow', this.getActualProgress());
            $('.progressBar .progress-bar span').text(this.getActualProgress() + '%');

            // show next btn
            if(this.showNext()) {
                //console.log('canShowNext');
                this.showHideNext();
                $('.progressContainer').show();
                $('.resultsPanel').hide();
                if(actualStep + 1 == steps) {
                    $('.btn.next').addClass('sendResults');
                } else {
                    $('.btn.next').removeClass('sendResults');
                }
            } else {
                //console.log('canNotShowNext');
                $('.btn.next').removeClass('sendResults');
                $('.btn.next').hide();
                $('.progressContainer').hide();
                $('.resultsPanel').show();
            }

            // show prev btn
            if(this.showPrev()) {
                //console.log('canShowPrev');
                $('.btn.back').show();
            } else {
                //console.log('canNotShowPrev');
                $('.btn.back').hide();
            }
		} else {
            questionBoxes.show();
            $('.btn.sendResults').show();
            $('.progressContainer').hide();

        }

	};
	
	this.showNext = function() {
		return actualStep < steps;
	};
	
	this.enableNext = function() {
		return this.showNext() && this.getAnswers().length > actualStep;
	};

	this.showHideSendResult = function() {
        if(questionBoxes.length == $('#quizForm input:checked').length) {
            $('.sendResults').removeAttr('disabled');
        } else {
            $('.sendResults').attr('disabled', 'disabled');
        }
    }

	this.showHideNext = function() {
		if(this.showNext() && this.enableNext()) {
			$('.btn.next').show();						
		} else {
			$('.btn.next').hide();
		}
	};
	
	this.showPrev = function() {
		return actualStep > 0;
	};
	
	this.goNext = function() {
		actualStep++;
		//console.log('goNext=' + actualStep);
		this.showActualStep();
	};
	
	this.goPrev = function() {
		actualStep--;
		//console.log('goPrev=' + actualStep);
		this.showActualStep();
	};
	
	this.showActualStep();
    this.showHideSendResult();
	this.generateFormId();
	this.setQuizId(qId);
}