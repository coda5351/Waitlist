import { defineConfig } from 'cypress';

export default defineConfig({
  e2e: {
    // Before running these specs you must start the Vite server manually:
    //
    //   npm run dev
    //
    // Cypress will check the `baseUrl` and wait for it to become available.
    // default port used by development server; change via PORT env if needed
    baseUrl: `http://localhost:${process.env.PORT || 5174}`,
    specPattern: 'cypress/e2e/**/*.cy.{js,ts}',
    supportFile: 'cypress/support/e2e.ts',
    video: false
  }
});
