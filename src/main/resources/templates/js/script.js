// event listener to respond to "Show another quote" button clicks
// when user clicks anywhere on the button, the "printQuote" function is called
document.getElementById('loadQuote').addEventListener("click", printQuote, false);

//Array of quotes
var quotes = [
  {
    quote: "Frankly, my dear, I don't give a damn.",
    source: "Rhett Butler",
    citation: "Gone with the Wind",
    year: 1939
  },
  {
    quote: "I'm gonna make him an offer he can't refuse.",
    source: "Vito Corleone",
    citation: "The Godfather",
    year: 1972
  },
  {
    quote: "Toto, I've a feeling we're not in Kansas anymore.",
    source: "Terry Malloy",
    citation: "The Wizard of Oz",
    year: 1939
  },
  {
    quote: "Go ahead, make my day.",
    source: "Harry Callahan",
    citation: "Sudden Impact",
    year: 1983
  },
  {
    quote: "May the Force be with you.",
    source: "Han Solo",
    citation: "Star Wars",
    year: 1977
  },
  {
    quote: "You talkin' to me?",
    source: "Travis Bickle",
    citation: "Taxi Driver",
    year: 1976
  },
  {
    quote: "Bond. James Bond.",
    source: "James Bond",
    citation: "Dr. No",
    year: 1962
  },
  {
    quote: "Why so serious?",
    source: "The Joker",
    citation: "The Dark Knight",
    year: 2008
  },
  {
    quote: "I'll be back." ,
    source: "The Terminator",
  },
  {
    quote: "Say hello to my little friend!" ,
    source: "Tony Montana",
    citation: "Scarface",
    year: 1983
  }
];

//Selects a random quote object from the quotes array
function getRandomQuote() {
  return quotes[ Math.floor( Math.random() * quotes.length ) ];
}

//Creates the message to display in html
function constructMessage(quoteObject) {
  //Creates the html to be displayed
  var message = '<p class="quote">'+quoteObject.quote+'</p>';
  message += '<p class="source">' + quoteObject.source;
  /* Citation and year attributes are optional.
  If statements include the attributes only if they are present. */
  if (quoteObject.citation != null) {
    message += '<span class="citation">'+ quoteObject.citation +'</span>';
  }
  if (quoteObject.year != null) {
    message += '<span class="year">'+ quoteObject.year +'</span>';
  }
  message += '</p>';
  return message;
}

//Prints the randomly selected quote to the page
function printQuote() {
  //Selects the random quote
  var quoteObject = getRandomQuote();

  //Constructs the message in html
  var message = constructMessage(quoteObject);

  //Displays the message in the page
  document.getElementById('quote-box').innerHTML = message;
}
