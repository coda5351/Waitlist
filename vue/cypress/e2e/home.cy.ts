describe('Home page', () => {
  it('renders welcome heading', () => {
    cy.visit('/');
    cy.contains('h2', 'Welcome');
  });
});
